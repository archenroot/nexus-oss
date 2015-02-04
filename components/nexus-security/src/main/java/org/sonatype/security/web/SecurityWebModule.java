/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.security.web;

import java.lang.reflect.Constructor;
import java.util.Enumeration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.sonatype.security.authentication.FirstSuccessfulModularRealmAuthenticator;
import org.sonatype.security.authorization.ExceptionCatchingModularRealmAuthorizer;

import com.google.common.base.Throwables;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authz.Authorizer;
import org.apache.shiro.config.ConfigurationException;
import org.apache.shiro.guice.web.ShiroWebModule;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.nexus.NexusDefaultWebSessionManager;
import org.apache.shiro.nexus.NexusWebSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;
import org.eclipse.sisu.inject.BeanLocator;

/**
 * Extends ShiroWebModule to configure commonly set components such as SessionDAO, Authenticator, Authorizer, etc.
 *
 * @since 2.6.1
 */
public class SecurityWebModule
    extends ShiroWebModule
{
  public SecurityWebModule(final ServletContext servletContext) {
    super(servletContext);
  }

  @Override
  protected void configureShiroWeb() {
    bindRealm().to(EmptyRealm.class); // not used in practice, just here to keep Shiro module happy

    // configure our preferred security components
    bind(SessionDAO.class).to(EnterpriseCacheSessionDAO.class).asEagerSingleton();
    bind(Authenticator.class).to(FirstSuccessfulModularRealmAuthenticator.class).in(Singleton.class);
    bind(Authorizer.class).to(ExceptionCatchingModularRealmAuthorizer.class).in(Singleton.class);
    bind(ProtectedPathManager.class).to(ProtectedPathManagerImpl.class).in(Singleton.class);

    // override the default resolver with one backed by a FilterChainManager using an injected filter map
    bind(FilterChainResolver.class).toConstructor(ctor(PathMatchingFilterChainResolver.class)).asEagerSingleton();
    bind(FilterChainManager.class).toProvider(FilterChainManagerProvider.class).in(Singleton.class);

    // bindings used by external modules
    expose(ProtectedPathManager.class);
    expose(FilterChainResolver.class);
  }

  @Override
  protected void bindWebSecurityManager(AnnotatedBindingBuilder<? super WebSecurityManager> bind) {
    bind(NexusWebSecurityManager.class).asEagerSingleton();

    // bind RealmSecurityManager and WebSecurityManager to _same_ component
    bind(RealmSecurityManager.class).to(NexusWebSecurityManager.class);
    bind.to(NexusWebSecurityManager.class);

    // bindings used by external modules
    expose(RealmSecurityManager.class);
    expose(WebSecurityManager.class);
  }

  @Override
  protected void bindSessionManager(AnnotatedBindingBuilder<SessionManager> bind) {
    // use native web session management instead of delegating to servlet container
    // workaround for NEXUS-5727, see NexusDefaultWebSessionManager javadoc for clues
    bind.to(NexusDefaultWebSessionManager.class).asEagerSingleton();
    // this is a PrivateModule, so explicitly binding the NexusDefaultSessionManager class
    bind(NexusDefaultWebSessionManager.class);
  }

  /**
   * Empty {@link Realm} - only used to satisfy Shiro's need for an initial realm binding.
   */
  @Singleton
  private static final class EmptyRealm
      implements Realm
  {
    public String getName() {
      return getClass().getName();
    }

    public boolean supports(AuthenticationToken token) {
      return false;
    }

    public AuthenticationInfo getAuthenticationInfo(AuthenticationToken token) {
      return null;
    }
  }

  /**
   * @return Public constructor with given parameterTypes; wraps checked exceptions
   */
  private static <T> Constructor<T> ctor(Class<T> clazz, Class<?>... parameterTypes) {
    try {
      return clazz.getConstructor(parameterTypes);
    }
    catch (Exception e) {
      Throwables.propagateIfPossible(e);
      throw new ConfigurationException(e);
    }
  }

  /**
   * Constructs a {@link DefaultFilterChainManager} from an injected {@link Filter} map.
   */
  private static final class FilterChainManagerProvider
      implements Provider<FilterChainManager>, Mediator<Named, Filter, FilterChainManager>
  {
    private final FilterConfig filterConfig;

    private final BeanLocator beanLocator;

    @Inject
    private FilterChainManagerProvider(@Named("SHIRO") ServletContext servletContext,
                                       BeanLocator beanLocator)
    {
      // simple configuration so we can initialize filters as we add them
      this.filterConfig = new SimpleFilterConfig("SHIRO", servletContext);
      this.beanLocator = beanLocator;
    }

    public FilterChainManager get() {
      FilterChainManager filterChainManager = new DefaultFilterChainManager(filterConfig);
      beanLocator.watch(Key.get(Filter.class, Named.class), this, filterChainManager);
      return filterChainManager;
    }

    public void add(final BeanEntry<Named, Filter> entry, final FilterChainManager manager) {
      manager.addFilter(entry.getKey().value(), entry.getValue(), true);
    }

    public void remove(final BeanEntry<Named, Filter> filter, final FilterChainManager manager) {
      // no-op
    }
  }

  /**
   * Simple {@link FilterConfig} that delegates to the surrounding {@link ServletContext}.
   */
  private static final class SimpleFilterConfig
      implements FilterConfig
  {
    private final String filterName;

    private final ServletContext servletContext;

    SimpleFilterConfig(String filterName, ServletContext servletContext) {
      this.filterName = filterName;
      this.servletContext = servletContext;
    }

    public String getFilterName() {
      return filterName;
    }

    public ServletContext getServletContext() {
      return servletContext;
    }

    public String getInitParameter(String name) {
      return servletContext.getInitParameter(name);
    }

    public Enumeration getInitParameterNames() {
      return servletContext.getInitParameterNames();
    }
  }
}
