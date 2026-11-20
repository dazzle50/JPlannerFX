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
    if ( !context.getGui().confirmUnsavedChanges( "New plan", "Do you want to save before starting new?" ) )
      return;

    // prepare fresh defaults before closing views or replacing the current plan
    var plan = new Plan();
    plan.initialise();
    context.getGui().closePlanWindows();
    context.replacePlan( plan );
    context.getStatus().update( Level.INFO, "New plan created" );
  }
}
