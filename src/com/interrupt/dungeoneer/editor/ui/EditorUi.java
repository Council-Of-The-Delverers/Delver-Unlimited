package com.interrupt.dungeoneer.editor.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.interrupt.api.steam.SteamApi;
import com.interrupt.dungeoneer.editor.Editor;
import com.interrupt.dungeoneer.editor.EditorFrame;
import com.interrupt.dungeoneer.editor.EditorRightClickEntitiesMenu;
import com.interrupt.dungeoneer.editor.EditorRightClickMenu;
import com.interrupt.dungeoneer.editor.EditorFrame.MoveMode;
import com.interrupt.dungeoneer.editor.ui.menu.MenuAccelerator;
import com.interrupt.dungeoneer.editor.ui.menu.MenuItem;
import com.interrupt.dungeoneer.editor.ui.menu.Scene2dMenu;
import com.interrupt.dungeoneer.editor.ui.menu.Scene2dMenuBar;
import com.interrupt.dungeoneer.entities.Entity;
import com.interrupt.dungeoneer.game.Game;
import com.interrupt.dungeoneer.game.Level;
import com.interrupt.dungeoneer.game.Level.Source;
import com.interrupt.dungeoneer.generator.RoomGenerator;
import net.cotd.delverunlimited.helper.history.History;
import net.cotd.delverunlimited.helper.history.HistoryHelper;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;
import javax.swing.JFrame;

public class EditorUi {
    Stage stage;
    Table mainTable;
    public static Skin defaultSkin;
    public static Skin mediumSkin;
    public static Skin smallSkin;
    private ScrollPane entityPropertiesPane = null;
    private PropertiesMenu propertiesMenu = null;
    private Table sidebarTable = null;
    private Cell propertiesCell = null;
    Scene2dMenu rightClickMenu;
    Scene2dMenuBar menuBar;
    ActionListener resizeWindowAction;
    ActionListener newWindowAction;
    ActionListener pickAction;
    ActionListener uploadModAction;
    ActionListener setThemeAction;
    ActionListener setFogSettingsAction;
    ActionListener openWorkingDir;
    static MenuItem historyBar;
    ActionListener openRightClickMenu;
    private Vector2 propertiesSize = new Vector2();
    private float rightClickTime;
    private EditorFrame editorFrame;
    Viewport viewport;
    private static String lastGeneratedLevelType = "DUNGEON";
    private static String lastGeneratedLevelRoomType = "DUNGEON_ROOMS";
    private static String lastGeneratedRoomType = "DUNGEON_ROOMS";

    private ActionListener makeLevelGeneratorAction(final String theme, final String roomGenerator, final EditorFrame editorFrame) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                editorFrame.getLevel().editorMarkers.clear();
                editorFrame.getLevel().entities.clear();
                editorFrame.getLevel().theme = theme;
                editorFrame.getLevel().generated = true;
                editorFrame.getLevel().dungeonLevel = 0;
                editorFrame.getLevel().crop(0, 0, 85, 85);
                editorFrame.getLevel().roomGeneratorChance = 0.4F;
                editorFrame.getLevel().roomGeneratorType = roomGenerator;
                editorFrame.getLevel().generate(Source.EDITOR);
                editorFrame.refresh();
                EditorUi.lastGeneratedLevelType = theme;
                EditorUi.lastGeneratedLevelRoomType = roomGenerator;
            }
        };
    }

    private ActionListener makeAnotherLevelGeneratorAction(final EditorFrame editorFrame) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                EditorUi.this.makeLevelGeneratorAction(EditorUi.lastGeneratedLevelType, EditorUi.lastGeneratedLevelRoomType, editorFrame).actionPerformed(actionEvent);
            }
        };
    }

    private ActionListener makeRoomGeneratorAction(final String generatorType, final EditorFrame editorFrame) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                editorFrame.getLevel().editorMarkers.clear();
                editorFrame.getLevel().entities.clear();
                Level generatedLevel = new Level(17, 17);
                generatedLevel.roomGeneratorType = generatorType;
                RoomGenerator g = new RoomGenerator(generatedLevel, generatorType);
                g.generate(true, true, true, true);
                editorFrame.getLevel().crop(0, 0, generatedLevel.width, generatedLevel.height);
                editorFrame.getLevel().paste(generatedLevel, 0, 0);
                editorFrame.refresh();
                EditorUi.lastGeneratedRoomType = generatorType;
            }
        };
    }

    private ActionListener makeAnotherRoomGeneratorAction(final EditorFrame editorFrame) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                EditorUi.this.makeRoomGeneratorAction(EditorUi.lastGeneratedRoomType, editorFrame).actionPerformed(actionEvent);
            }
        };
    }

    private static ActionListener openRightClickMenu(History history) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                File file = new File(history.levelAbsolutePath);
                Editor.openInEditor(file, true);
            }
        };
    }

    private static ActionListener clearHistory(MenuItem item) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                item.subMenu.removeAll();
                updateHistory(true);
            }
        };
    }

    public EditorUi(final Editor editor, final EditorFrame editorFrame) {
        defaultSkin = new Skin(Game.getInternal("ui/editor/HoloSkin/Holo-dark-hdpi.json"), new TextureAtlas(Game.getInternal("ui/editor/HoloSkin/Holo-dark-hdpi.atlas")));
        mediumSkin = new Skin(Game.getInternal("ui/editor/HoloSkin/Holo-dark-mdpi.json"), new TextureAtlas(Game.getInternal("ui/editor/HoloSkin/Holo-dark-mdpi.atlas")));
        smallSkin = new Skin(Game.getInternal("ui/editor/HoloSkin/Holo-dark-ldpi.json"), new TextureAtlas(Game.getInternal("ui/editor/HoloSkin/Holo-dark-ldpi.atlas")));
        this.viewport = new FillViewport((float)Gdx.graphics.getWidth(), (float)Gdx.graphics.getHeight());
        this.stage = new Stage(this.viewport);
        this.mainTable = new Table();
        this.mainTable.setFillParent(true);
        this.mainTable.align(10);
        this.editorFrame = editorFrame;
        this.newWindowAction = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                NewLevelDialog newLevelDialog = new NewLevelDialog(EditorUi.smallSkin) {
                    protected void result(Object object) {
                        if ((Boolean)object) {
                            editorFrame.createNewLevel(this.getLevelWidth(), this.getLevelHeight());
                            editor.createdNewLevel();
                        }

                    }
                };
                newLevelDialog.show(EditorUi.this.stage);
            }
        };
        this.resizeWindowAction = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                NewLevelDialog newLevelDialog = new NewLevelDialog(EditorUi.smallSkin) {
                    protected void result(Object object) {
                        if ((Boolean)object) {
                            editorFrame.resizeLevel(this.getLevelWidth(), this.getLevelHeight());
                        }

                    }
                };
                newLevelDialog.show(EditorUi.this.stage);
            }
        };
        this.uploadModAction = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                UploadModDialog uploadModDialog = new UploadModDialog("Upload Mod to Steam Workshop", EditorUi.smallSkin);
                uploadModDialog.show(EditorUi.this.stage);
            }
        };
        this.setFogSettingsAction = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                SetFogDialog fogDialog = new SetFogDialog(EditorUi.smallSkin, editorFrame.getLevel()) {
                    protected void result(Object object) {
                    }
                };
                fogDialog.show(EditorUi.this.stage);
            }
        };
        this.setThemeAction = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                SetThemeDialog themeDialog = new SetThemeDialog(EditorUi.smallSkin, editorFrame.getLevel()) {
                    protected void result(Object object) {
                        editorFrame.getLevel().theme = this.getSelectedTheme();
                    }
                };
                themeDialog.show(EditorUi.this.stage);
            }
        };
        this.pickAction = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                editorFrame.doPick();
            }
        };
        this.openWorkingDir = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    if (editor.currentDirectory.length() > 5) {
                        File dirToOpen = new File(editor.currentDirectory);
                        Desktop desktop = Desktop.getDesktop();


                        desktop.open(dirToOpen);
                    }

                } catch (Exception ex) {
                    Gdx.app.error("EditorUi", ex.getMessage());
                }
            }
        };
        this.menuBar = new Scene2dMenuBar(smallSkin);
        this.menuBar.addItem(new MenuItem("File", smallSkin)
                .addItem(new MenuItem("New", smallSkin, this.newWindowAction).setAccelerator(new MenuAccelerator(42, true, false)))
                .addSeparator()
                .addItem(new MenuItem("Open", smallSkin, editor.openAction).setAccelerator(new MenuAccelerator(43, true, false)))
                .addItem(new MenuItem("Open working directory", smallSkin, this.openWorkingDir))
                .addSeparator()
                .addItem(new MenuItem("Save", smallSkin, editor.saveAction).setAccelerator(new MenuAccelerator(47, true, false)))
                .addItem(new MenuItem("Save As . . .", smallSkin, editor.saveAsAction).setAccelerator(new MenuAccelerator(47, true, true)))
                .addSeparator()
                .addItem(new MenuItem("Exit", smallSkin, editor.exitAction)));

        this.menuBar.addItem(new MenuItem("Edit", smallSkin)
                .addItem(new MenuItem("Undo", smallSkin, editor.undoAction).setAccelerator(new MenuAccelerator(54, true, false)))
                .addItem(new MenuItem("Redo", smallSkin, editor.redoAction).setAccelerator(new MenuAccelerator(53, true, false)))
                .addItem(new MenuItem("Copy", smallSkin, editor.copyAction).setAccelerator(new MenuAccelerator(31, true, false)))
                .addItem(new MenuItem("Paste", smallSkin, editor.pasteAction).setAccelerator(new MenuAccelerator(50, true, false)))
                .addSeparator()
                .addItem(new MenuItem("Carve Tiles", smallSkin, editor.carveAction).setAccelerator(new MenuAccelerator(66, false, false)))
                .addItem(new MenuItem("Paint Tiles", smallSkin, editor.paintAction).setAccelerator(new MenuAccelerator(66, false, true)))
                .addItem(new MenuItem("Delete", smallSkin, editor.deleteAction).setAccelerator(new MenuAccelerator(67, false, false)))
                .addItem(new MenuItem("Pick Textures", smallSkin, this.pickAction).setAccelerator(new MenuAccelerator(35, false, false)))
                .addItem(new MenuItem("Reset Selection", smallSkin, editor.escapeAction).setAccelerator(new MenuAccelerator(131, false, false)))
                .addSeparator()
                .addItem(new MenuItem("Height Edit Mode", smallSkin)
                        .addItem(new MenuItem("Plane", smallSkin, editor.planeHeightAction))
                        .addItem(new MenuItem("Vertex", smallSkin, editor.vertexHeightAction))
                        .addItem(new MenuItem("Toggle", smallSkin, editor.vertexToggleAction).setAccelerator(new MenuAccelerator(50, false, false))))

                .addItem(new MenuItem("Rotate Texture", smallSkin)
                        .addItem(new MenuItem("Floor", smallSkin, editor.rotateFloorTexAction).setAccelerator(new MenuAccelerator(48, false, false)))
                        .addItem(new MenuItem("Ceiling", smallSkin, editor.rotateCeilTexAction).setAccelerator(new MenuAccelerator(48, false, true))))

                .addItem(new MenuItem("Rotate Wall Angle", smallSkin, editor.rotateWallAngle).setAccelerator(new MenuAccelerator(49, false, false)))
                .addItem(new MenuItem("Flatten", smallSkin)
                        .addItem(new MenuItem("Floor", smallSkin, editor.flattenFloor).setAccelerator(new MenuAccelerator(34, false, false)))
                        .addItem(new MenuItem("Ceiling", smallSkin, editor.flattenCeiling).setAccelerator(new MenuAccelerator(34, false, true))))
                .addSeparator()
                .addItem(new MenuItem("Rotate Level", smallSkin)
                        .addItem(new MenuItem("Clockwise", smallSkin, editor.rotateLeftAction))
                        .addItem(new MenuItem("Counter-Clockwise", smallSkin, editor.rotateRightAction)))
                .addItem(new MenuItem("Move Mode", smallSkin)
                        .addItem(new MenuItem("Clamp X", smallSkin, editor.xDragMode).setAccelerator(new MenuAccelerator(52, false, false)))
                        .addItem(new MenuItem("Clamp Y", smallSkin, editor.yDragMode).setAccelerator(new MenuAccelerator(53, false, false)))
                        .addItem(new MenuItem("Clamp Z", smallSkin, editor.zDragMode).setAccelerator(new MenuAccelerator(54, false, false)))
                        .addItem(new MenuItem("Rotate", smallSkin, editor.rotateMode).setAccelerator(new MenuAccelerator(46, false, false))))

                .addItem(new MenuItem("Generate Room", smallSkin, makeAnotherRoomGeneratorAction(editorFrame)).setAccelerator(new MenuAccelerator(35, false, true))
                        .addItem(new MenuItem("Dungeon Room", smallSkin, makeRoomGeneratorAction("DUNGEON_ROOMS", editorFrame)))
                        .addItem(new MenuItem("Cave Room", smallSkin, makeRoomGeneratorAction("CAVE_ROOMS", editorFrame)))
                        .addItem(new MenuItem("Sewer Room", smallSkin, makeRoomGeneratorAction("SEWER_ROOMS", editorFrame)))
                        .addItem(new MenuItem("Temple Room", smallSkin, makeRoomGeneratorAction("TEMPLE_ROOMS", editorFrame))))

                .addItem(new MenuItem("Generate Level", smallSkin, makeAnotherLevelGeneratorAction(editorFrame))
                        .addItem(new MenuItem("Dungeon", smallSkin, makeLevelGeneratorAction("DUNGEON", "DUNGEON_ROOMS", editorFrame)))
                        .addItem(new MenuItem("Cave", smallSkin, makeLevelGeneratorAction("CAVE", "CAVE_ROOMS", editorFrame)))
                        .addItem(new MenuItem("Sewer", smallSkin, makeLevelGeneratorAction("SEWER", "SEWER_ROOMS", editorFrame)))
                        .addItem(new MenuItem("Temple", smallSkin, makeLevelGeneratorAction("UNDEAD", "TEMPLE_ROOMS", editorFrame)))));

        this.menuBar.addItem(new MenuItem("View", smallSkin)

                .addItem(new MenuItem("Toggle Simulation", smallSkin, editor.toggleSimulation).setAccelerator(new MenuAccelerator(30, false, false)))
                .addItem(new MenuItem("Toggle Collision Boxes", smallSkin, editor.toggleCollisionBoxesAction))
                .addItem(new MenuItem("Toggle Lights", smallSkin, editor.toggleLightsAction).setAccelerator(new MenuAccelerator(40, false, false))));

        this.menuBar.addItem(new MenuItem("Level", smallSkin)

                .addItem(new MenuItem("Test Level", smallSkin, editor.playAction).setAccelerator(new MenuAccelerator(44, false, false)))
                .addSeparator()
                .addItem(new MenuItem("Resize Level", smallSkin, this.resizeWindowAction))
                .addItem(new MenuItem("Set Theme", smallSkin, this.setThemeAction))
                .addItem(new MenuItem("Set Fog Settings", smallSkin, this.setFogSettingsAction)));
        if (SteamApi.api.isAvailable()) {
            this.menuBar.addItem(new MenuItem("Mods", smallSkin)
                    .addItem(new MenuItem("Upload Mod to Workshop", smallSkin, this.uploadModAction)));
        } else {
            Gdx.app.log("SteamApi", "The Steam API has not been started, uploading to workshop disabled!");
        }

        historyBar = new MenuItem("History", smallSkin);
        updateHistory(false);

        menuBar.addItem(historyBar);
        menuBar.pack();

        this.mainTable.setZIndex(1000);
        this.mainTable.add(this.menuBar);
        this.stage.addListener(new InputListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                Actor touched = EditorUi.this.stage.hit(x, y, false);
                if (!(event.getTarget() instanceof TextField)) {
                    EditorUi.this.stage.setKeyboardFocus((Actor)null);
                }

                if (touched == null || !touched.isDescendantOf(EditorUi.this.menuBar)) {
                    EditorUi.this.menuBar.close();
                }

                if (button == 0 && touched == EditorUi.this.mainTable && EditorUi.this.entityPropertiesPane != null && editorFrame.getHoveredEntity() == null) {
                    EditorUi.this.sidebarTable.setVisible(false);
                }

                if (button == 0 && (touched == null || EditorUi.this.rightClickMenu != null && !touched.isDescendantOf(EditorUi.this.rightClickMenu))) {
                    EditorUi.this.hideContextMenu();
                } else if (button == 1) {
                    EditorUi.this.rightClickTime = editorFrame.time;
                }

                return touched != EditorUi.this.mainTable;
            }

            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == 131) {

                    for (Object actor : EditorUi.this.stage.getActors()) {
                        if (actor instanceof TextureRegionPicker) {
                            TextureRegionPicker picker = (TextureRegionPicker) actor;
                            picker.hide();
                            return true;
                        }

                        if (actor instanceof Dialog) {
                            ((Dialog) actor).hide();
                            return true;
                        }
                    }
                }

                return false;
            }
        });
    }

    public static void updateHistory(boolean clear)
    {
        if (historyBar.subMenu != null) {
            historyBar.subMenu.removeAll();
        }

        if (clear) {
            FileHandle file = Gdx.files.local(HistoryHelper.HISTORY_FILE_PATH);
            file.delete();
        }

        /* Read from json */
        History[] data = HistoryHelper.getObjectJson();
        if (data != null) {
            for (History history : data) {
                if (history.verify()) {
                    historyBar.addItem(new MenuItem(history.toString(), smallSkin, openRightClickMenu(history)));
                }
            }
        }

        /* Add clear all button */
        historyBar.addSeparator();
        historyBar.addItem(new MenuItem("Clear All", smallSkin, clearHistory(historyBar)));
    }

    public void showEntityPropertiesMenu(EditorFrame editorFrame) {
        this.showEntityPropertiesMenu(editorFrame, true);
    }

    public void showEntityPropertiesMenu(EditorFrame editorFrame, boolean resetScroll) {
        if (editorFrame.getPickedEntity() != null) {
            Array<Entity> selected = new Array();
            selected.add(editorFrame.getPickedEntity());
            selected.addAll(editorFrame.getAdditionalSelectedEntities());
            this.propertiesMenu = new PropertiesMenu(smallSkin, editorFrame, selected);
            if (this.entityPropertiesPane == null) {
                this.sidebarTable = new Table(smallSkin);
                this.sidebarTable.setBackground("scroll_opaque_gray");
                this.entityPropertiesPane = new ScrollPane(this.propertiesMenu, smallSkin, "opaque-gray");
                this.entityPropertiesPane.setFadeScrollBars(false);
                this.entityPropertiesPane.setFlickScroll(false);
                this.entityPropertiesPane.setClamp(true);
                this.sidebarTable.align(2);
                this.propertiesCell = this.sidebarTable.add(this.entityPropertiesPane);
                this.stage.addActor(this.sidebarTable);
            } else {
                this.entityPropertiesPane.setWidget(this.propertiesMenu);
            }

            if (resetScroll) {
                this.entityPropertiesPane.setScrollY(0.0F);
            }

            this.propertiesSize.set(this.propertiesMenu.getWidth(), this.propertiesMenu.getHeight());
            this.resize(this.stage.getWidth(), this.stage.getHeight());
            this.sidebarTable.setVisible(true);
            this.stage.setScrollFocus(this.entityPropertiesPane);
        } else if (this.entityPropertiesPane != null) {
            this.sidebarTable.setVisible(false);
            this.entityPropertiesPane.setScrollY(0.0F);
        }

    }

    public void resize(float width, float height) {
        this.viewport.setWorldSize(width, height);
        this.viewport.update((int)width, (int)height, true);
        this.mainTable.pack();
        if (this.entityPropertiesPane != null && this.propertiesMenu != null) {
            boolean fillsStage = this.propertiesSize.y > this.stage.getHeight() - this.menuBar.getHeight();
            this.entityPropertiesPane.setSize(this.propertiesSize.x + (fillsStage ? 60.0F : 30.0F), this.propertiesSize.y);
            this.sidebarTable.setSize(this.entityPropertiesPane.getWidth(), this.stage.getHeight() - this.menuBar.getHeight());
            this.sidebarTable.setX(this.stage.getWidth() - this.sidebarTable.getWidth());
            this.sidebarTable.setY(0.0F);
            this.propertiesCell.width(this.entityPropertiesPane.getWidth());
        }

    }

    public void initUi() {
        this.stage.addActor(this.mainTable);
    }

    public void showContextMenu(float x, float y) {
        this.stage.addActor(this.rightClickMenu);
        this.rightClickMenu.setPosition(x, y);
    }

    public void showContextMenu(float x, float y, Scene2dMenu contextMenu) {
        this.rightClickMenu = contextMenu;
        this.stage.addActor(this.rightClickMenu);
        this.rightClickMenu.setPosition(x, y);
    }

    public void hideContextMenu() {
        this.stage.getRoot().removeActor(this.rightClickMenu);
    }

    public Stage getStage() {
        return this.stage;
    }

    public static Skin getSkin() {
        return defaultSkin;
    }

    public static Skin getMediumSkin() {
        return mediumSkin;
    }

    public static Skin getSmallSkin() {
        return smallSkin;
    }

    public void touchUp(int x, int y, int pointer, int button) {
        if (button == 1) {
            this.hideContextMenu();
            y = (int)this.stage.getHeight() - y;
            float currentTime = this.editorFrame.time;
            if ((double)(currentTime - this.rightClickTime) > 0.5D) {
                return;
            }

            if (this.editorFrame.getPickedEntity() == null && this.editorFrame.getHoveredEntity() == null) {
                this.editorFrame.setSelected(true);
                this.showContextMenu((float)x, (float)y, new EditorRightClickEntitiesMenu(smallSkin, (float)this.editorFrame.getSelectionX(), (float)this.editorFrame.getSelectionY(), this.editorFrame, this.editorFrame.getLevel()));
            } else {
                Entity sel = this.editorFrame.getPickedEntity();
                if (sel == null) {
                    sel = this.editorFrame.getHoveredEntity();
                }

                if (this.editorFrame.getMoveMode() == MoveMode.ROTATE) {
                    this.editorFrame.clearEntitySelection();
                } else {
                    EditorRightClickMenu menu;
                    if (this.editorFrame.getAdditionalSelectedEntities().size == 0) {
                        menu = new EditorRightClickMenu(sel, this.editorFrame, (JFrame)null, this.editorFrame.getLevel());
                        this.showContextMenu((float)x, (float)y, menu);
                    } else {
                        menu = new EditorRightClickMenu(sel, this.editorFrame.getAdditionalSelectedEntities(), this.editorFrame, (JFrame)null, this.editorFrame.getLevel());
                        this.showContextMenu((float)x, (float)y, menu);
                    }
                }
            }
        }

    }
}
