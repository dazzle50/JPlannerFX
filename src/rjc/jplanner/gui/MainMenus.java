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

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import rjc.jplanner.gui.menu.EditMenu;
import rjc.jplanner.gui.menu.FileMenu;
import rjc.jplanner.gui.menu.HelpMenu;
import rjc.jplanner.gui.menu.ReportMenu;
import rjc.jplanner.gui.menu.ViewMenu;
import rjc.table.view.editor.AbstractCellEditor;

/*************************************************************************************************/
/*********************************** Main application menu bar ***********************************/
/*************************************************************************************************/

public class MainMenus extends MenuBar
{
  private Menu m_menuFile;
  private Menu m_menuEdit;
  private Menu m_menuReport;
  private Menu m_menuView;
  private Menu m_menuHelp;

  /***************************************** constructor *****************************************/
  public MainMenus()
  {
    // construct main window menu bar
    m_menuFile = new FileMenu();
    m_menuEdit = new EditMenu();
    m_menuReport = new ReportMenu();
    m_menuView = new ViewMenu();
    m_menuHelp = new HelpMenu();
    getMenus().addAll( m_menuFile, m_menuEdit, m_menuReport, m_menuView, m_menuHelp );

    // without using setOnShowing() so it is free for other uses, add handler when menu about to be shown
    getMenus().forEach( menu -> menu.addEventHandler( Menu.ON_SHOWING, event -> onMenuShowing() ) );
  }

  /***************************************** onMenuShow ******************************************/
  private void onMenuShowing()
  {
    // if any table cell editing in progress, end it
    AbstractCellEditor.endEditing();
  }

}
