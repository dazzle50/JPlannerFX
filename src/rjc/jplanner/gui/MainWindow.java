/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
 *  https://github.com/dazzle50/JPlannerFX                                *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.jplanner.gui;

import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import rjc.table.undo.UndoStackWindow;

/*************************************************************************************************/
/************************** Main JPlannerFX application window contents **************************/
/*************************************************************************************************/

public class MainWindow extends Scene
{
  private MenuBar         m_menus;      // menus at top of MainWindow
  private TabPane         m_tabs;       // tabs containing application functionality
  private TextField       m_statusBar;  // status bar at bottom of MainWindow
  private UndoStackWindow m_undoWindow; // window to show undo-plan

  /**************************************** constructor ******************************************/
  public MainWindow()
  {
    // main window based on grid layout
    super( new GridPane() );

    // prepare components
    m_menus = new MainMenus();
    m_tabs = new MainTabs();
    m_statusBar = new TextField();

    // arrange the grid
    GridPane grid = (GridPane) getRoot();
    grid.add( m_menus, 0, 0 );
    grid.add( m_tabs, 0, 1 );
    grid.add( m_statusBar, 0, 2 );
    GridPane.setHgrow( m_tabs, Priority.ALWAYS );
    GridPane.setVgrow( m_tabs, Priority.ALWAYS );
  }

}
