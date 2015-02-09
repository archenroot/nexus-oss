/*
 * Copyright (c) 2008-2015 Sonatype, Inc.
 *
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package com.sonatype.nexus.repository.nuget.internal.odata;

import java.util.Date;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.Maps;
import org.codehaus.plexus.util.StringUtils;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.expression.OrderByExpression.Direction;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.jpa.JPASkipToken;
import org.odata4j.producer.jpa.JPQLGenerator;

import static com.google.common.base.Strings.nullToEmpty;
import static org.odata4j.producer.resources.OptionsQueryParser.parseFilter;
import static org.odata4j.producer.resources.OptionsQueryParser.parseOrderBy;
import static org.odata4j.producer.resources.OptionsQueryParser.parseSkip;
import static org.odata4j.producer.resources.OptionsQueryParser.parseSkipToken;
import static org.odata4j.producer.resources.OptionsQueryParser.parseTop;

/**
 * Utility methods for working with OData/SQL expressions.
 */
public final class ODataUtils
{
  public static final Map<String, String> COLUMN_ALIASES = columnAliases();

  // ----------------------------------------------------------------------

  public static final int PAGE_SIZE = 40;

  // ----------------------------------------------------------------------

  private static final DateTimeFormatter ISO_PARSER =
      ISODateTimeFormat.dateTimeParser().withLocale(Locale.ENGLISH).withZoneUTC();

  // ----------------------------------------------------------------------

  public static long datetime(final String millis) {
    return ISO_PARSER.parseMillis(millis);
  }

  public static Date toDate(final String isoFormatDate) {
    return new Date(datetime(isoFormatDate));
  }

  // ----------------------------------------------------------------------

  /**
   * Converts the given OData query and select clause into an SQL expression.
   *
   * @param query OData parameters
   * @param count True if the intention is to merely count the items rather than itemizing them
   */
  public static ComponentQuery query(final Map<String, String> query, final boolean count) {
    ComponentQuery.Builder q = new ComponentQuery.Builder();

    // TODO: parameters should be case-insensitive

    boolean hasTerms = false;
    for (String term : StringUtils.strip(nullToEmpty(query.get("searchTerm")), "\" '").split("[+\\s]+")) {
      if (StringUtils.isNotBlank(term)) {
        term = '%' + term + '%';
        if (!hasTerms) {
          q.where("(");
          hasTerms = true;
        }
        else {
          q.where(" OR ");
        }
        q.where("id LIKE ").param(term);
        q.where(" OR title LIKE ").param(term);
        q.where(" OR description LIKE ").param(term);
        q.where(" OR tags LIKE ").param(term);
        q.where(" OR authors LIKE ").param(term);
      }
    }
    if (hasTerms) {
      q.where(")");
    }

    final String id = StringUtils.strip(query.get("id"), "\" '");
    if (id != null) {
      if (q.hasWhere()) {
        q.where(" AND ");
      }
      q.where(nugat("id") + " = ").param("id", id);
    }

    if ("false".equalsIgnoreCase(StringUtils.strip(query.get("includePrerelease"), "\" '"))) {
      if (q.hasWhere()) {
        q.where(" AND ");
      }
      q.where(" " + nugat("prerelease") + "=false ");
    }

    final QueryInfo odata;
    try {
      odata =
          new QueryInfo(null, parseTop(query.get("$top")),
              parseSkip(query.get("$skip")),
              parseFilter(query.get("$filter")),
              parseOrderBy(query.get("$orderby")),
              parseSkipToken(query.get("$skiptoken")),
              // unused arguments
              null, null, null);
    }
    catch (final RuntimeException e) {
      throw new IllegalArgumentException("Bad Request - Error in query syntax.", e);
    }

    final JPQLGenerator generator = new JPQLGenerator(null, null, COLUMN_ALIASES);
    if (odata.filter != null) {
      if (q.hasWhere()) {
        q.where(" AND ");
      }
      q.where("(").where(generator.toJpql(odata.filter)).where(")");
    }
    if (odata.skipToken != null) {
      if (q.hasWhere()) {
        q.where(" AND ");
      }
      q.where(" (").where(generator.toJpql(JPASkipToken.parse(null, odata.orderBy, odata.skipToken,
          "id", "version"))).where(")");
    }

    if (!count) {
      q.suffix(" ORDER BY ");
      if (null != odata.orderBy) {
        for (int i = 0, size = odata.orderBy.size(); i < size; i++) {
          final OrderByExpression o = odata.orderBy.get(i);
          q.suffix(generator.toJpql(o.getExpression()));
          // Orientdb doesn't support an implied sort direction
          if (o.getDirection() == Direction.ASCENDING) {
            q.suffix(" ASC");
          }
          else if (o.getDirection() == Direction.DESCENDING) {
            q.suffix(" DESC");
          }
          q.suffix(", ");
        }
      }
      // Tack on the default ordering
      q.suffix("id asc, version asc");

      int top = PAGE_SIZE;
      if (odata.top != null && odata.top.intValue() < top) {
        top = odata.top.intValue();
      }
      q.suffix(" LIMIT " + top);
      if (odata.skip != null) {
        q.suffix(" OFFSET " + odata.skip);
      }
    }
    return q.build();
  }

  private static Map<String, String> columnAliases() {
    Map<String, String> aliases = Maps.newHashMap();
    aliases.put("ID", nugat("id"));
    aliases.put("VERSION", nugat("version"));
    aliases.put("ISPRERELEASE", nugat("is_prerelease"));
    aliases.put("ISLATESTVERSION",nugat("is_latest_version"));
    aliases.put("ISABSOLUTELATESTVERSION",nugat("is_absolute_latest_version"));
    aliases.put("CREATED", nugat("created"));
    aliases.put("LASTUPDATED", nugat("last_updated"));
    aliases.put("PUBLISHED", nugat("published"));
    aliases.put("PACKAGESIZE", nugat("package_size"));
    aliases.put("PACKAGE_HASH", nugat("package_hash"));
    aliases.put("PACKAGEHASHALGORITHM", nugat("package_hash_algorithm"));

    aliases.put("TITLE", nugat("title"));
    aliases.put("SUMMARY", nugat("summary"));
    aliases.put("REQUIRELICENSEACCEPTANCE", nugat("requires_license_acceptance"));

    aliases.put("VERSIONDOWNLOADCOUNT", nugat("version_download_count"));
    aliases.put("DOWNLOADCOUNT", nugat("download_count"));

    return aliases;
  }

  /**
   * Converts an ODATA element name into the name of the json attribute we store it under in orient.
   */
  private static String nugat(final String column) {
    return "attributes.nuget." + column;
  }

}
