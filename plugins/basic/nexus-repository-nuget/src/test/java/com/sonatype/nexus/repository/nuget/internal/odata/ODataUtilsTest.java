package com.sonatype.nexus.repository.nuget.internal.odata;

import java.util.Map;

import com.google.common.collect.Maps;
import org.junit.Test;

public class ODataUtilsTest
{
  @Test
  public void testQuery() throws Exception {

    Map<String, String> query = Maps.newHashMap();
    query.put("$filter", "IsLatestVersion");
    query.put("$orderby", "DownloadCount desc,Id");
    query.put("$skip", "0");
    query.put("$top", "30");
    query.put("searchTerm", "'jilted'");
    query.put("targetFramework", "'net45'");
    query.put("includePrerelease", "false");

    final ComponentQuery foo = ODataUtils.query(query, false);

    System.err.println(foo.getWhere());
    System.err.println(foo.getQuerySuffix());
    System.err.println(foo.getParameters());
  }
}