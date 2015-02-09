package com.sonatype.nexus.repository.nuget.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.inject.Named;

import com.sonatype.nexus.repository.nuget.internal.odata.ComponentQuery;
import com.sonatype.nexus.repository.nuget.internal.odata.NuspecSplicer;
import com.sonatype.nexus.repository.nuget.internal.odata.ODataFeedUtils;
import com.sonatype.nexus.repository.nuget.internal.odata.ODataTemplates;
import com.sonatype.nexus.repository.nuget.internal.odata.ODataUtils;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.hash.MultiHashingInputStream;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.util.NestedAttributesMap;
import org.sonatype.nexus.repository.view.Parameters;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import org.apache.commons.codec.binary.Base64;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;
import org.odata4j.producer.InlineCount;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static org.odata4j.producer.resources.OptionsQueryParser.parseInlineCount;
import static org.odata4j.producer.resources.OptionsQueryParser.parseSkip;
import static org.odata4j.producer.resources.OptionsQueryParser.parseTop;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_FORMAT;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_NAME;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_VERSION;

/**
 * @since 3.0
 */
@Named("local")
public class NugetGalleryFacetImpl
    extends FacetSupport
    implements NugetGalleryFacet
{

  public static final String P_LOCATION = "location";

  public static final String NUGET = "nuget";

  private StorageFacet storage;

  public static final String NO_NAMESPACES = "";

  private static final VersionScheme SCHEME = new GenericVersionScheme();

  // ----------------------------------------------------------------------

  @Deprecated
  private static final String TABLE = "PACKAGES";

  @Deprecated
  private static final String IS_MEMBER = " LOCATE(CONCAT(CONCAT(':',REPOSITORYID),':'),?1) ";

  @Deprecated
  private static final String ISABSOLUTELATESTVERSION = ",MAX_VERSION(VERSION) AS AV";

  @Deprecated
  private static final String ISLATESTVERSION = ",MAX_VERSION(CASEWHEN(ISPRERELEASE,'',VERSION)) AS V";

  @Deprecated
  private static final String JOIN_VERSIONS = "(SELECT ID" + ISABSOLUTELATESTVERSION + ISLATESTVERSION + " FROM "
      + TABLE + " WHERE" + IS_MEMBER + "GROUP BY ID) AS L INNER JOIN " + TABLE + " AS P ON L.ID=P.ID";

  @Deprecated
  private static final String ALL_MEMBERS = JOIN_VERSIONS + " WHERE" + IS_MEMBER;

  @Deprecated
  private static final String SELECT_MEMBERS =
      "SELECT P.*,(P.VERSION=L.AV) AS ISABSOLUTELATESTVERSION,(P.VERSION=L.V) AS ISLATESTVERSION FROM " + ALL_MEMBERS;

  @Override
  protected void doConfigure() throws Exception {
    storage = getRepository().facet(StorageFacet.class);
  }

  @Override
  protected void doDestroy() throws Exception {
    storage = null;
  }

  @Override
  @Guarded(by = STARTED)
  public int count(final String path, final Parameters parameters) {
    Map<String, String> query = asMap(parameters);
    log.debug("Count: " + query);

    final ComponentQuery componentQuery = ODataUtils.query(query, true);

    try (StorageTx storageTx = storage.openTx()) {

      int count = executeCount(componentQuery, storageTx);

      final Integer skip = parseSkip(query.get("$skip"));
      if (null != skip && skip >= 0) {
        // If we were asked to skip some values, deduct this from the total count reported by the query
        count = Math.max(0, count - skip);
      }
      final Integer top = parseTop(query.get("$top"));
      if (null != top && top >= 0) {
        // If we were asked for the top 'n' values, then cap the count at no more than this value
        count = Math.min(count, top.intValue());
      }

      storageTx.commit();
      return count;
    }
  }

  @Override
  @Guarded(by = STARTED)
  public String feed(final String base, final String name, final Parameters parameters) {
    Map<String, String> query = asMap(parameters);

    log.debug("Select: " + query);

    final Map<String, String> extra =
        ImmutableMap.of("BASEURI", base, "ENDPOINT", name, "LASTUPDATED",
            ODataFeedUtils.datetime(System.currentTimeMillis()), "NAMESPACES", NO_NAMESPACES);

    final StringBuilder xml = new StringBuilder();
    xml.append(ODataTemplates.interpolate(ODataTemplates.NUGET_FEED, extra));


    // NEXUS-6822 Visual Studio doesn't send a sort order by default, leading to unusable results
    if (!query.containsKey("$orderby")) {
      query.put("$orderby", "DownloadCount desc");
    }

    ComponentQuery componentQuery = ODataUtils.query(query, false);

    try (StorageTx storageTx = storage.openTx()) {

      // NXCM-4502 add inlinecount only if requested
      if (inlineCountRequested(query)) {
        int inlineCount = executeCount(componentQuery, storageTx);
        xml.append(ODataTemplates.interpolate(ODataTemplates.NUGET_INLINECOUNT,
            ImmutableMap.of("COUNT", String.valueOf(inlineCount))));
      }

      final Iterable<OrientVertex> components = storageTx.findComponents(componentQuery.getWhere(),
          componentQuery.getParameters(), getRepositories(), componentQuery.getQuerySuffix());

      int n = 0;
      for (OrientVertex component : components) {
        n++;

        final Map<String, ?> data = toData(component, extra);

        xml.append(ODataTemplates.interpolate(ODataTemplates.NUGET_ENTRY, data));
        if (n == ODataUtils.PAGE_SIZE) {
          xml.append("  <link rel=\"next\" href=\"").append(base).append('/').append(name);
          xml.append('?').append(ODataFeedUtils.skipLink(data, query)).append("\"/>\n");
          break;
        }
      }

      storageTx.commit();
    }

    return xml.append("</feed>").toString();
  }

  private Map<String, String> asMap(final Parameters parameters) {
    Map<String, String> query = Maps.newHashMap();
    for (String param : parameters.names()) {
      query.put(param, parameters.get(param));
    }
    return query;
  }

  private Map<String, ?> toData(final OrientVertex component, Map<String, String> extra) {
    Map<String, Object> data = Maps.newHashMap();

    // TODO: Put all the Vertex properties in there

    for (String key : extra.keySet()) {
      data.put(key, extra.get(key));
    }

    return data;
  }

  @Override
  @Guarded(by = STARTED)
  public String entry(final String base, final String id, final String version) {
    return null;
  }

  @Override
  @Guarded(by = STARTED)
  public String locate(final String id, final String version) {
    return null;
  }

  @Override
  @Guarded(by = STARTED)
  public String[] identify(final String location) {
    return new String[0];
  }

  @Override
  @Guarded(by = STARTED)
  public void put(final InputStream inputStream) throws IOException, NugetPackageException {
    try (StorageTx storageTx = storage.openTx()) {

      // We need to read the blob content multiple times; this is temporary storage and it's not important that it's a blob
      final BlobRef tempBlobRef = storageTx.createBlob(inputStream,
          ImmutableMap.of(BlobStore.BLOB_NAME_HEADER, "", BlobStore.CREATED_BY_HEADER, "unknown"));

      final Blob tempBlob = checkNotNull(storageTx.getBlob(tempBlobRef));

      Map<String, String> metadata = determineMetadata(tempBlob);

      final OrientVertex bucket = storageTx.getBucket();
      final OrientVertex component = createOrUpdateComponent(storageTx, bucket, metadata);

      try (InputStream in = tempBlob.getInputStream()) {
        createOrUpdateAsset(storageTx, bucket, component, in);
      }
      // TODO: Add whatever other properties we care about for nuget assets

      maintainAggregateInfo(storageTx, metadata.get("ID"));

      storageTx.deleteBlob(tempBlobRef);
      storageTx.commit();
    }
  }

  private String createBlobName(OrientVertex component) {
    return component.getProperty("name") + " " + component.getProperty("version") + "@" + getRepository().getName();
  }

  /**
   * Ensure all the components for a given 'id':
   * - have up to date latest version/absolute latest version fields.
   * - have up to date aggregate download count info
   * (updating download counts is different for hosted and proxies; proxies possibly don't need to..)
   */
  private void maintainAggregateInfo(final StorageTx storageTx, final String id) {
    long totalDownloadCount = 0;

    SortedSet<OrientVertex> releases = Sets.newTreeSet(new ComponentVersionComparator());
    SortedSet<OrientVertex> allReleases = Sets.newTreeSet(new ComponentVersionComparator());

    final Iterable<OrientVertex> components = findComponentsById(storageTx, id);
    for (OrientVertex component : components) {

      final NestedAttributesMap nugetAttributes = storageTx.getAttributes(component).child(NUGET);
      final Boolean is_prerelease = nugetAttributes.get("is_prerelease", Boolean.class);
      if (!is_prerelease) {
        releases.add(component);
      }
      allReleases.add(component);

      final Integer versionDownloadCount = nugetAttributes.get("version_download_count", Integer.class);
      totalDownloadCount += checkNotNull(versionDownloadCount);
    }

    // set "download_count", "latest_version", "absolute_latest_version" fields

    OrientVertex latestVersion = releases.isEmpty() ? null : releases.last();
    OrientVertex absoluteLatestVersion = allReleases.isEmpty() ? null : releases.last();

    for (OrientVertex component : allReleases) {
      final NestedAttributesMap nugetAttributes = storageTx.getAttributes(component).child(NUGET);

      nugetAttributes.set("is_latest_version", component.equals(latestVersion));
      nugetAttributes.set("is_absolute_latest_version", component.equals(absoluteLatestVersion));

      nugetAttributes.set("download_count", totalDownloadCount);
    }
  }

  private Iterable<OrientVertex> findComponentsById(final StorageTx storageTx, final Object id) {
    final String whereClause = "name = :name";
    Map<String, Object> parameters = ImmutableMap.of("name", id);
    return storageTx.findComponents(whereClause, parameters, getRepositories(), null);
  }

  private OrientVertex createOrUpdateAsset(final StorageTx storageTx, final OrientVertex bucket,
                                           final OrientVertex component, final InputStream in)
  {
    OrientVertex asset = null;

    final List<OrientVertex> assets = storageTx.findAssets(component);
    if (assets.isEmpty()) {
      asset = storageTx.createAsset(bucket);
      asset.addEdge(StorageFacet.E_PART_OF_COMPONENT, component);
    }
    else {
      asset = assets.get(0);
    }

    final ImmutableMap<String, String> headers = ImmutableMap
        .of(BlobStore.BLOB_NAME_HEADER, createBlobName(component), BlobStore.CREATED_BY_HEADER, "unknown");

    storageTx.setBlob(in, headers, asset, Arrays.asList(HashAlgorithm.SHA512), "application/zip");

    return asset;
  }

  /**
   * Determine the metadata for a .nupkg Blob.
   * - nuspec data (comes from .nuspec)
   * - size (package size)
   * - hash(es) (package hash sha-512)
   * - publication dates (current date or...)
   */
  private Map<String, String> determineMetadata(final Blob tempBlob) throws NugetPackageException {
    try (InputStream inputStream = tempBlob.getInputStream()) {
      MultiHashingInputStream hashingStream = new MultiHashingInputStream(Arrays.asList(HashAlgorithm.SHA512),
          inputStream);
      Map<String, String> metadata = NuspecSplicer.extractNuspecData(hashingStream);

      metadata.put("PACKAGESIZE", String.valueOf(hashingStream.count()));
      HashCode code = hashingStream.hashes().get(HashAlgorithm.SHA512);
      metadata.put("PACKAGEHASH", new String(Base64.encodeBase64(code.asBytes()), Charsets.UTF_8));
      metadata.put("PACKAGEHASHALGORITHM", "SHA512");

      // Note: These are defaults that hold for locally-published packages,
      // but should be overridden for remotely fetched content
      final String creationTime = ODataFeedUtils.datetime(System.currentTimeMillis());
      metadata.put("CREATED", creationTime);
      metadata.put("LASTUPDATED", creationTime);
      metadata.put("PUBLISHED", creationTime);

      return metadata;
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
    catch (XmlPullParserException e) {
      throw new NugetPackageException("Unable to read .nuspec from package stream", e);
    }
  }

  private OrientVertex createOrUpdateComponent(final StorageTx storageTx, final OrientVertex bucket,
                                               final Map<String, String> data)
  {
    final String id = checkNotNull(data.get("ID"));
    final String version = checkNotNull(data.get("VERSION"));

    final OrientVertex component = findOrCreateComponent(storageTx, bucket, id, version);

    final NestedAttributesMap attributes = storageTx.getAttributes(component);
    final NestedAttributesMap nugetAttr = attributes.child(NUGET);

    // Force the version download count to zero if it wasn't provided nor previously set
    if (!data.containsKey("VERSIONDOWNLOADCOUNT") && !nugetAttr.contains("version_download_count")) {
      data.put("VERSIONDOWNLOADCOUNT", "0");
    }

    nugetAttr.set("id", data.get("ID"));

    try {
      SCHEME.parseVersion(data.get("VERSION"));
    }
    catch (InvalidVersionSpecificationException e) {
      throw new IllegalArgumentException("Bad version syntax: " + data.get("VERSION"));
    }
    nugetAttr.set("version", data.get("VERSION"));

    nugetAttr.set(P_LOCATION, data.get("LOCATION"));
    nugetAttr.set("authors", data.get("AUTHORS"));
    nugetAttr.set("copyright", data.get("COPYRIGHT"));
    nugetAttr.set("created", data.get("CREATED"));
    nugetAttr.set("dependencies", data.get("DEPENDENCIES"));
    nugetAttr.set("description", data.get("DESCRIPTION"));
    if (data.containsKey("DOWNLOADCOUNT")) {
      // for proxies, take whatever they give us here. for hosted, it will be computed later anyway
      nugetAttr.set("download_count", Integer.parseInt(data.get("DOWNLOADCOUNT")));
    }
    nugetAttr.set("gallery_details_url", data.get("GALLERYDETAILSURL"));
    nugetAttr.set("icon_url", data.get("ICONURL"));
    nugetAttr.set("is_prerelease", Boolean.parseBoolean(data.get("ISPRERELEASE")));
    nugetAttr.set("last_updated", data.get("LASTUPDATED"));
    nugetAttr.set("published", data.get("PUBLISHED"));
    nugetAttr.set("language", data.get("LANGUAGE"));
    nugetAttr.set("license_url", data.get("LICENSEURL"));
    nugetAttr.set("package_hash", data.get("PACKAGEHASH"));
    nugetAttr.set("package_hash_algorithm", data.get("PACKAGEHASHALGORITHM"));
    nugetAttr.set("package_size", data.get("PACKAGESIZE"));
    nugetAttr.set("project_url", data.get("PROJECTURL"));
    nugetAttr.set("report_abuse_url", data.get("REPORTABUSEURL"));
    nugetAttr.set("release_notes", data.get("RELEASENOTES"));
    nugetAttr.set("require_license_acceptance", Boolean.parseBoolean(data.get("REQUIRELICENSEACCEPTANCE")));
    nugetAttr.set("summary", data.get("SUMMARY"));
    nugetAttr.set("tags", data.get("TAGS"));
    nugetAttr.set("title", data.get("TITLE"));
    if (data.containsKey("VERSIONDOWNLOADCOUNT")) {
      nugetAttr.set("version_download_count", Integer.parseInt(data.get("VERSIONDOWNLOADCOUNT")));
    }

    return component;
  }

  private OrientVertex findOrCreateComponent(final StorageTx storageTx, final OrientVertex bucket, final String name,
                                             final String version)
  {
    final OrientVertex found = findComponent(storageTx, name, version);
    if (found != null) {
      return found;
    }

    return createComponent(storageTx, bucket, name, version);
  }

  private OrientVertex findComponent(final StorageTx storageTx, final String name, final Object version) {
    final ImmutableMap<String, Object> params = ImmutableMap.of("version", version, "name", name);

    final Iterable<OrientVertex> components = storageTx
        .findComponents("version=:version AND name=:name", params, getRepositories(), null);

    final Iterator<OrientVertex> iterator = components.iterator();
    if (iterator.hasNext()) {
      return iterator.next();
    }
    else {
      return null;
    }
  }

  private OrientVertex createComponent(final StorageTx storageTx, final OrientVertex bucket, final String name,
                                       final String version)
  {
    log.debug("Creating NuGet component {} v. {}", name, version);
    OrientVertex component = storageTx.createComponent(storageTx.getBucket());
    component.setProperty(P_FORMAT, NugetFormat.NAME);
    component.setProperty(P_NAME, name);
    // Nuget components don't have a group
    component.setProperty(P_VERSION, version);
    return component;
  }

  private boolean inlineCountRequested(Map<String, String> query) {
    return InlineCount.ALLPAGES.equals(parseInlineCount(query.get("$inlinecount")));
  }

  private int executeCount(final ComponentQuery query, final StorageTx storageTx)
  {
    return (int) storageTx.countComponents(query.getWhere(), query.getParameters(), getRepositories(),
        query.getQuerySuffix());
  }


  private List<Repository> getRepositories() {
    // TODO: Consider groups
    return asList(getRepository());
  }

  private static class ComponentVersionComparator
      implements Comparator<OrientVertex>
  {
    @Override
    public int compare(final OrientVertex o1, final OrientVertex o2) {
      try {
        Version v1 = SCHEME.parseVersion((String) o1.getProperty("version"));
        Version v2 = SCHEME.parseVersion((String) o2.getProperty("version"));
        return v1.compareTo(v2);
      }
      catch (InvalidVersionSpecificationException e) {
        throw Throwables.propagate(e);
      }
    }
  }
}
