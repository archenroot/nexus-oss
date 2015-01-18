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
/*global Ext*/

/**
 * Abstract settings panel.
 *
 * @since 3.0
 */
Ext.define('NX.view.SettingsPanel', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-settingsPanel',
  ui: 'nx-inset',

  layout: {
    type: 'vbox',
    align: 'stretch',
    pack: 'start'
  },

  autoScroll: true,

  // TODO maxWidth: 1024,

  defaults: {
    style: {
      margin: '0px 0px 20px 0px'
    }
  },

  /**
   * @public
   * Loads an Ext.data.Model into this form (internally just calls NX.view.SettingsForm.loadRecord)
   * @param model The model to load
   */
  loadRecord: function(model) {
    var me = this,
        settingsForm = me.down('nx-settingsform');

    if (settingsForm) {
      settingsForm.loadRecord(model);
    }
  }

});
