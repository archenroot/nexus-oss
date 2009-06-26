package org.sonatype.nexus.configuration;

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.validator.InvalidConfigurationException;

/**
 * Helper abstract class to implement configurable components to "click" them in into generic configuration environment.
 * 
 * @author cstamas
 */
public abstract class AbstractConfigurable
    extends AbstractLogEnabled
    implements Configurable
{
    /** The configuration */
    private CoreConfiguration repositoryConfiguration;

    protected abstract ApplicationConfiguration getApplicationConfiguration();

    // Configurable iface

    public final CoreConfiguration getCurrentCoreConfiguration()
    {
        return repositoryConfiguration;
    }

    protected Object getCurrentConfiguration( boolean forWrite )
    {
        return repositoryConfiguration.getConfiguration( forWrite );
    }

    protected ExternalConfiguration getExternalConfiguration()
    {
        return getCurrentCoreConfiguration().getExternalConfiguration();
    }

    public final void validateConfiguration( Object config )
        throws ConfigurationException
    {
        if ( config == null )
        {
            throw new InvalidConfigurationException( "This configuration is null!" );
        }

        doValidateConfiguration( config );
    }

    public final void configure( Object config )
        throws ConfigurationException
    {
        validateConfiguration( config );

        if ( config instanceof CoreConfiguration )
        {
            this.repositoryConfiguration = (CoreConfiguration) config;
        }
        else
        {
            this.repositoryConfiguration = wrapConfiguration( config );
        }

        doConfigure( false );
    }

    public final void configure()
        throws ConfigurationException
    {
        doConfigure( true );
    }

    public boolean isDirty()
    {
        return getCurrentCoreConfiguration().isDirty()
            || getCurrentCoreConfiguration().getExternalConfiguration().isDirty();
    }

    protected void doValidateConfiguration( Object config )
        throws ConfigurationException
    {
        if ( getValidator() != null )
        {
            getValidator().validate( getApplicationConfiguration(), config );
        }
    }

    protected void doConfigure( boolean validate )
        throws ConfigurationException
    {
        if ( validate )
        {
            doValidateConfiguration( getCurrentConfiguration( false ) );
        }

        if ( getConfigurator() != null )
        {
            getConfigurator().applyConfiguration( this, getApplicationConfiguration(), getCurrentCoreConfiguration() );
        }

        getCurrentCoreConfiguration().applyChanges();
    }

    protected abstract Configurator getConfigurator();
    
    protected abstract Validator getValidator();

    protected abstract CoreConfiguration wrapConfiguration( Object configuration );
}
