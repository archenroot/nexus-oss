package com.sonatype.nexus.repository.nuget.internal;

import java.nio.charset.Charset;
import java.util.Map;

import javax.annotation.Nonnull;

import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.sisu.goodies.common.ComponentSupport;

/**
 * @since 3.0
 */
public class NugetGalleryHandler
    extends ComponentSupport
    implements Handler
{
  public static final String FEED_CONTENT_TYPE = "application/xml";

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    final State state = context.getAttributes().get(State.class);
    final Map<String, String> tokens = state.getTokens();
    final String operation = tokens.get("operation");

    final NugetGalleryFacet facet = context.getRepository().facet(NugetGalleryFacet.class);

    final String base = getRepositoryUri(context);

    final String feed = facet.feed(base, operation, context.getRequest().getParameters());

    return HttpResponses.ok(new StringPayload(feed, Charset.forName("UTF-8"), FEED_CONTENT_TYPE));
  }

  private String getRepositoryUri(final Context context) {
    // TODO: Implement this correctly
    return context.getRepository().getName();
  }
}
