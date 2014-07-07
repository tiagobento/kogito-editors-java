/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.screens.guided.scorecard.client.editor;

import java.util.List;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.New;
import javax.inject.Inject;

import com.google.gwt.user.client.ui.IsWidget;
import org.drools.workbench.models.guided.scorecard.shared.ScoreCardModel;
import org.drools.workbench.screens.guided.scorecard.client.resources.i18n.GuidedScoreCardConstants;
import org.drools.workbench.screens.guided.scorecard.client.type.GuidedScoreCardResourceType;
import org.drools.workbench.screens.guided.scorecard.model.ScoreCardModelContent;
import org.drools.workbench.screens.guided.scorecard.service.GuidedScoreCardEditorService;
import org.guvnor.common.services.shared.metadata.MetadataService;
import org.guvnor.common.services.shared.validation.model.ValidationMessage;
import org.guvnor.common.services.shared.version.events.RestoreEvent;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.kie.uberfire.client.callbacks.DefaultErrorCallback;
import org.kie.uberfire.client.callbacks.HasBusyIndicatorDefaultErrorCallback;
import org.kie.uberfire.client.common.MultiPageEditor;
import org.kie.uberfire.client.common.Page;
import org.kie.workbench.common.services.datamodel.model.PackageDataModelOracleBaselinePayload;
import org.kie.workbench.common.widgets.client.callbacks.CommandBuilder;
import org.kie.workbench.common.widgets.client.callbacks.CommandDrivenErrorCallback;
import org.kie.workbench.common.widgets.client.datamodel.AsyncPackageDataModelOracle;
import org.kie.workbench.common.widgets.client.datamodel.AsyncPackageDataModelOracleFactory;
import org.kie.workbench.common.widgets.client.datamodel.ImportAddedEvent;
import org.kie.workbench.common.widgets.client.datamodel.ImportRemovedEvent;
import org.kie.workbench.common.widgets.client.menu.FileMenuBuilder;
import org.kie.workbench.common.widgets.client.popups.file.CommandWithCommitMessage;
import org.kie.workbench.common.widgets.client.popups.file.SaveOperationService;
import org.kie.workbench.common.widgets.client.popups.validation.DefaultFileNameValidator;
import org.kie.workbench.common.widgets.client.popups.validation.ValidationPopup;
import org.kie.workbench.common.widgets.client.resources.i18n.CommonConstants;
import org.kie.workbench.common.widgets.configresource.client.widget.bound.ImportsWidgetPresenter;
import org.kie.workbench.common.widgets.metadata.client.callbacks.MetadataSuccessCallback;
import org.kie.workbench.common.widgets.metadata.client.widget.MetadataWidget;
import org.kie.workbench.common.widgets.viewsource.client.callbacks.ViewSourceSuccessCallback;
import org.kie.workbench.common.widgets.viewsource.client.screen.ViewSourceView;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.backend.vfs.Path;
import org.uberfire.client.annotations.WorkbenchEditor;
import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.workbench.events.ChangeTitleWidgetEvent;
import org.uberfire.lifecycle.IsDirty;
import org.uberfire.lifecycle.OnClose;
import org.uberfire.lifecycle.OnMayClose;
import org.uberfire.lifecycle.OnSave;
import org.uberfire.lifecycle.OnStartup;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.ParameterizedCommand;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.workbench.events.NotificationEvent;
import org.uberfire.workbench.model.menu.Menus;
import org.uberfire.workbench.type.FileNameUtil;

import static org.kie.uberfire.client.common.ConcurrentChangePopup.*;

@Dependent
@WorkbenchEditor(identifier = "GuidedScoreCardEditor", supportedTypes = { GuidedScoreCardResourceType.class })
public class GuidedScoreCardEditorPresenter {

    @Inject
    private Caller<GuidedScoreCardEditorService> scoreCardEditorService;

    @Inject
    private Caller<MetadataService> metadataService;

    @Inject
    private GuidedScoreCardEditorView view;

    @Inject
    private ViewSourceView viewSource;

    @Inject
    private MetadataWidget metadataWidget;

    @Inject
    private MultiPageEditor multiPage;

    @Inject
    private Event<NotificationEvent> notification;

    @Inject
    private Event<ChangeTitleWidgetEvent> changeTitleNotification;

    @Inject
    private Event<RestoreEvent> restoreEvent;

    @Inject
    private PlaceManager placeManager;

    @Inject
    private GuidedScoreCardResourceType type;

    @Inject
    private AsyncPackageDataModelOracleFactory oracleFactory;

    @Inject
    private DefaultFileNameValidator fileNameValidator;

    @Inject
    @New
    private FileMenuBuilder menuBuilder;
    private Menus menus;

    private ObservablePath path;
    private PlaceRequest place;
    private boolean isReadOnly;
    private String version;
    private ObservablePath.OnConcurrentUpdateEvent concurrentUpdateSessionInfo = null;

    private ScoreCardModel model;
    private AsyncPackageDataModelOracle oracle;

    @Inject
    private ImportsWidgetPresenter importsWidget;

    @OnStartup
    public void onStartup( final ObservablePath path,
                           final PlaceRequest place ) {
        this.path = path;
        this.place = place;
        this.isReadOnly = place.getParameter( "readOnly", null ) == null ? false : true;
        this.version = place.getParameter( "version", null );

        this.path.onRename( new Command() {
            @Override
            public void execute() {
                changeTitleNotification.fire( new ChangeTitleWidgetEvent( place, getTitle(), null ) );
            }
        } );
        this.path.onConcurrentUpdate( new ParameterizedCommand<ObservablePath.OnConcurrentUpdateEvent>() {
            @Override
            public void execute( final ObservablePath.OnConcurrentUpdateEvent eventInfo ) {
                concurrentUpdateSessionInfo = eventInfo;
            }
        } );

        this.path.onConcurrentRename( new ParameterizedCommand<ObservablePath.OnConcurrentRenameEvent>() {
            @Override
            public void execute( final ObservablePath.OnConcurrentRenameEvent info ) {
                newConcurrentRename( info.getSource(),
                                     info.getTarget(),
                                     info.getIdentity(),
                                     new Command() {
                                         @Override
                                         public void execute() {
                                             disableMenus();
                                         }
                                     },
                                     new Command() {
                                         @Override
                                         public void execute() {
                                             reload();
                                         }
                                     }
                                   ).show();
            }
        } );

        this.path.onConcurrentDelete( new ParameterizedCommand<ObservablePath.OnConcurrentDelete>() {
            @Override
            public void execute( final ObservablePath.OnConcurrentDelete info ) {
                newConcurrentDelete( info.getPath(),
                                     info.getIdentity(),
                                     new Command() {
                                         @Override
                                         public void execute() {
                                             disableMenus();
                                         }
                                     },
                                     new Command() {
                                         @Override
                                         public void execute() {
                                             placeManager.closePlace( place );
                                         }
                                     }
                                   ).show();
            }
        } );

        makeMenuBar();

        view.showBusyIndicator( CommonConstants.INSTANCE.Loading() );

        loadContent();
    }

    private void reload() {
        concurrentUpdateSessionInfo = null;
        changeTitleNotification.fire( new ChangeTitleWidgetEvent( place, getTitle(), null ) );
        view.showBusyIndicator( CommonConstants.INSTANCE.Loading() );
        loadContent();
    }

    private void disableMenus() {
        menus.getItemsMap().get( FileMenuBuilder.MenuItems.COPY ).setEnabled( false );
        menus.getItemsMap().get( FileMenuBuilder.MenuItems.RENAME ).setEnabled( false );
        menus.getItemsMap().get( FileMenuBuilder.MenuItems.DELETE ).setEnabled( false );
        menus.getItemsMap().get( FileMenuBuilder.MenuItems.VALIDATE ).setEnabled( false );
    }

    private void loadContent() {
        scoreCardEditorService.call( getModelSuccessCallback(),
                                     new CommandDrivenErrorCallback( view,
                                                                     new CommandBuilder().addNoSuchFileException( view,
                                                                                                                  multiPage,
                                                                                                                  menus ).build()
                                     ) ).loadContent( path );
    }

    private RemoteCallback<ScoreCardModelContent> getModelSuccessCallback() {
        return new RemoteCallback<ScoreCardModelContent>() {

            @Override
            public void callback( final ScoreCardModelContent content ) {
                //Path is set to null when the Editor is closed (which can happen before async calls complete).
                if ( path == null ) {
                    return;
                }

                multiPage.clear();
                multiPage.addWidget( view,
                                     CommonConstants.INSTANCE.EditTabTitle() );

                multiPage.addPage( new Page( viewSource,
                                             CommonConstants.INSTANCE.SourceTabTitle() ) {
                    @Override
                    public void onFocus() {
                        viewSource.showBusyIndicator( CommonConstants.INSTANCE.Loading() );
                        scoreCardEditorService.call( new ViewSourceSuccessCallback( viewSource ),
                                                     new HasBusyIndicatorDefaultErrorCallback( viewSource ) ).toSource( path,
                                                                                                                        view.getModel() );
                    }

                    @Override
                    public void onLostFocus() {
                        viewSource.clear();
                    }
                } );

                multiPage.addWidget( importsWidget,
                                     CommonConstants.INSTANCE.ConfigTabTitle() );

                multiPage.addPage( new Page( metadataWidget,
                                             CommonConstants.INSTANCE.MetadataTabTitle() ) {
                    @Override
                    public void onFocus() {
                            metadataWidget.showBusyIndicator( CommonConstants.INSTANCE.Loading() );
                            metadataService.call( new MetadataSuccessCallback( metadataWidget,
                                                                               isReadOnly ),
                                                  new HasBusyIndicatorDefaultErrorCallback( metadataWidget )
                                                ).getMetadata( path );
                    }

                    @Override
                    public void onLostFocus() {
                        //Nothing to do
                    }
                } );

                model = content.getModel();
                final PackageDataModelOracleBaselinePayload dataModel = content.getDataModel();
                oracle = oracleFactory.makeAsyncPackageDataModelOracle( path,
                                                                        model,
                                                                        dataModel );

                view.setContent( model,
                                 oracle );
                importsWidget.setContent( oracle,
                                          model.getImports(),
                                          isReadOnly );

                view.hideBusyIndicator();
            }
        };
    }

    private void makeMenuBar() {
        if ( isReadOnly ) {
            menus = menuBuilder.addRestoreVersion( path ).build();
        } else {
            menus = menuBuilder
                    .addSave( new Command() {
                        @Override
                        public void execute() {
                            onSave();
                        }
                    } )
                    .addCopy( path,
                              fileNameValidator )
                    .addRename( path,
                                fileNameValidator )
                    .addDelete( path )
                    .addValidate( onValidate() )
                    .build();
        }
    }

    public void handleImportAddedEvent( @Observes ImportAddedEvent event ) {
        if ( !event.getDataModelOracle().equals( this.oracle ) ) {
            return;
        }
        view.refreshFactTypes();
    }

    public void handleImportRemovedEvent( @Observes ImportRemovedEvent event ) {
        if ( !event.getDataModelOracle().equals( this.oracle ) ) {
            return;
        }
        view.refreshFactTypes();
    }

    private Command onValidate() {
        return new Command() {
            @Override
            public void execute() {
                scoreCardEditorService.call( new RemoteCallback<List<ValidationMessage>>() {
                    @Override
                    public void callback( final List<ValidationMessage> results ) {
                        if ( results == null || results.isEmpty() ) {
                            notification.fire( new NotificationEvent( CommonConstants.INSTANCE.ItemValidatedSuccessfully(),
                                                                      NotificationEvent.NotificationType.SUCCESS ) );
                        } else {
                            ValidationPopup.showMessages( results );
                        }
                    }
                }, new DefaultErrorCallback() ).validate( path,
                                                          view.getModel() );
            }
        };
    }

    @OnSave
    public void onSave() {
        if ( isReadOnly ) {
            view.alertReadOnly();
            return;
        }

        if ( concurrentUpdateSessionInfo != null ) {
            newConcurrentUpdate( concurrentUpdateSessionInfo.getPath(),
                                 concurrentUpdateSessionInfo.getIdentity(),
                                 new Command() {
                                     @Override
                                     public void execute() {
                                         save();
                                     }
                                 },
                                 new Command() {
                                     @Override
                                     public void execute() {
                                         //cancel?
                                     }
                                 },
                                 new Command() {
                                     @Override
                                     public void execute() {
                                         reload();
                                     }
                                 }
                               ).show();
        } else {
            save();
        }
    }

    private void save() {
        new SaveOperationService().save( path,
                                         new CommandWithCommitMessage() {
                                             @Override
                                             public void execute( final String comment ) {
                                                 view.showBusyIndicator( CommonConstants.INSTANCE.Saving() );
                                                 scoreCardEditorService.call( getSaveSuccessCallback(),
                                                                              new HasBusyIndicatorDefaultErrorCallback( view ) ).save( path,
                                                                                                                                       view.getModel(),
                                                                                                                                       metadataWidget.getContent(),
                                                                                                                                       comment );
                                             }
                                         }
                                       );
        concurrentUpdateSessionInfo = null;
    }

    private RemoteCallback<Path> getSaveSuccessCallback() {
        return new RemoteCallback<Path>() {

            @Override
            public void callback( final Path path ) {
                view.setNotDirty();
                view.hideBusyIndicator();
                metadataWidget.resetDirty();
                notification.fire( new NotificationEvent( CommonConstants.INSTANCE.ItemSavedSuccessfully() ) );
            }
        };
    }

    @WorkbenchPartView
    public IsWidget getWidget() {
        return multiPage;
    }

    @OnClose
    public void onClose() {
        this.path = null;
        this.oracleFactory.destroy( oracle );
    }

    @IsDirty
    public boolean isDirty() {
        if ( isReadOnly ) {
            return false;
        }
        return ( view.isDirty() || metadataWidget.isDirty() );
    }

    @OnMayClose
    public boolean checkIfDirty() {
        if ( isDirty() ) {
            return view.confirmClose();
        }
        return true;
    }

    @WorkbenchPartTitle
    public String getTitle() {
        String fileName = FileNameUtil.removeExtension( path,
                                                        type );
        if ( version != null ) {
            fileName = fileName + " v" + version;
        }

        if ( isReadOnly ) {
            return "Read Only Score Card Viewer [" + fileName + "]";
        }
        return GuidedScoreCardConstants.INSTANCE.ScoreCardEditorTitle() + " [" + fileName + "]";
    }

    @WorkbenchMenu
    public Menus getMenus() {
        return menus;
    }

    public void onRestore( final @Observes RestoreEvent restore ) {
        if ( path == null || restore == null || restore.getPath() == null ) {
            return;
        }
        if ( path.equals( restore.getPath() ) ) {
            loadContent();
            notification.fire( new NotificationEvent( CommonConstants.INSTANCE.ItemRestored() ) );
        }
    }

}
