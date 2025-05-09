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

import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import rjc.jplanner.Main;
import rjc.jplanner.gui.MainTabs;

/*************************************************************************************************/
/**************************** View menu for main application menu bar ****************************/
/*************************************************************************************************/

public class ViewMenu extends Menu
{

  /***************************************** constructor *****************************************/
  public ViewMenu()
  {
    // construct view menu for main window menu bar
    setText( "View" );

    getItems().add( undoWindow() );
    getItems().add( newWindow() );
  }

  /****************************************** viewMenu *******************************************/
  private CheckMenuItem undoWindow()
  {
    // show/hide undostack window
    var viewUndoStack = new CheckMenuItem( "Undo Stack..." );
    var undoWindow = Main.getUndoWindow();
    undoWindow.showingProperty().addListener( ( property, oldShow, newShow ) -> viewUndoStack.setSelected( newShow ) );
    viewUndoStack.setOnAction( event ->
    {
      if ( undoWindow.isShowing() )
        undoWindow.hide();
      else
      {
        undoWindow.show();
        undoWindow.toFront();
      }
    } );
    return viewUndoStack;
  }

  /****************************************** viewMenu *******************************************/
  private MenuItem newWindow()
  {
    // create new window
    var viewNewWindow = new MenuItem( "New Window..." );
    viewNewWindow.setOnAction( event ->
    {
      Stage stage = new Stage();
      stage.setTitle( "JPlannerFX" );
      stage.getIcons().add( Main.getIcon() );
      stage.setScene( new Scene( new MainTabs( false ) ) );
      stage.setWidth( 1000 );
      stage.setHeight( 600 );
      stage.show();
    } );
    return viewNewWindow;
  }

}
