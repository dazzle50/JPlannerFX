/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
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

package rjc.jplanner.storage;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.plan.Plan;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/********************** Creates a new plan after resolving unsaved changes ***********************/
/*************************************************************************************************/

public class FileNew
{
  /***************************************** constructor *****************************************/
  public FileNew( PlanContext context )
  {
    // commit active edits before checking cleanliness, including keyboard-triggered actions
    context.getGui().finishEditing();
    if ( !context.getUndoStack().isClean() )
    {
      var save = new ButtonType( "Save", ButtonData.YES );
      var saveAs = new ButtonType( "Save As", ButtonData.OTHER );
      var discard = new ButtonType( "Discard", ButtonData.NO );
      var alert = new Alert( AlertType.CONFIRMATION, null, save, saveAs, discard, ButtonType.CANCEL );
      alert.initOwner( context.getGui().getWindow() );
      alert.setTitle( "New plan" );
      alert.setHeaderText( "Do you want to save before starting new?" );

      // keep the declared button order without gaps between button groups
      var buttonBar = (ButtonBar) alert.getDialogPane().lookup( ".button-bar" );
      buttonBar.setButtonOrder( ButtonBar.BUTTON_ORDER_NONE );

      ButtonType choice = alert.showAndWait().orElse( ButtonType.CANCEL );
      if ( choice == ButtonType.CANCEL )
        return;
      if ( choice == save && !FileSave.saveCurrent( context ) )
        return;
      if ( choice == saveAs && !new FileSaveAs( context ).isSuccessful() )
        return;
    }

    // prepare fresh defaults before closing views or replacing the current plan
    var plan = new Plan();
    plan.initialise();
    context.getGui().closePlanWindows();
    context.replacePlan( plan );
    context.getStatus().update( Level.INFO, "New plan created" );
  }
}
