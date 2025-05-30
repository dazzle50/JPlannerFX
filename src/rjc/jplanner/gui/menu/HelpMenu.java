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

package rjc.jplanner.gui.menu;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import rjc.jplanner.Main;

/*************************************************************************************************/
/**************************** Help menu for main application menu bar ****************************/
/*************************************************************************************************/

public class HelpMenu extends Menu
{

  /***************************************** constructor *****************************************/
  public HelpMenu()
  {
    // construct help menu for main window menu bar
    setText( "Help" );

    getItems().add( about() );
  }

  /******************************************** about ********************************************/
  private MenuItem about()
  {
    // TODO simple about dialog
    MenuItem helpAbout = new MenuItem( "About JPlannerFX " + Main.VERSION );
    helpAbout.setDisable( true );
    return helpAbout;
  }

}
