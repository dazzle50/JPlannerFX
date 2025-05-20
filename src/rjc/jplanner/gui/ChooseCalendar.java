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

import rjc.jplanner.plan.Plan;
import rjc.table.Utils;
import rjc.table.control.ChooseField;

/*************************************************************************************************/
/****************** Control for choosing a plan calendar from a drop-down list *******************/
/*************************************************************************************************/

public class ChooseCalendar extends ChooseField
{

  /**************************************** constructor ******************************************/
  public ChooseCalendar()
  {
    // create a choose-field populated with the plan calendar names
    super( Plan.getCalendars().getNameArray() );
  }

  /*************************************** refreshChoices ****************************************/
  public void refreshChoices()
  {
    // TODO refresh choices as list of calendar names might have changed
    Utils.trace( "NOT YET IMPLEMENTED" );
  }

}
