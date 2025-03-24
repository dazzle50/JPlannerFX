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

/*************************************************************************************************/
/*********************************** Main application menu bar ***********************************/
/*************************************************************************************************/

import javafx.scene.control.MenuBar;

public class MainMenus extends MenuBar
{
  private Menu m_menuFile;
  private Menu m_menuEdit;
  private Menu m_menuReport;
  private Menu m_menuView;
  private Menu m_menuHelp;

  /**************************************** constructor ******************************************/
  public MainMenus()
  {
    // construct main window menu bar
    m_menuFile = new Menu( "File" );
    m_menuEdit = new Menu( "Edit" );
    m_menuReport = new Menu( "Report" );
    m_menuView = new Menu( "View" );
    m_menuHelp = new Menu( "Help" );

    getMenus().addAll( m_menuFile, m_menuEdit, m_menuReport, m_menuView, m_menuHelp );
  }
}
