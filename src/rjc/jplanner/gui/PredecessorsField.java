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

package rjc.jplanner.gui;

import rjc.jplanner.plan.TimeSpan;
import rjc.table.control.ExpandingField;

/*************************************************************************************************/
/********************** Expanding text field for entering task predecessors **********************/
/*************************************************************************************************/

public class PredecessorsField extends ExpandingField
{

  /**************************************** constructor ******************************************/
  public PredecessorsField()
  {
    // build TIME_UNITS regex from TimeSpan.Unit enum similar to "(?:mo|m|h|d|w|%)";
    StringBuilder TIME_UNITS = new StringBuilder();
    TIME_UNITS.append( "(?:" );
    for ( TimeSpan.Unit unit : TimeSpan.Unit.values() )
      TIME_UNITS.append( unit.abbreviation() ).append( '|' );
    TIME_UNITS.append( "%)" );

    final String NUMBER = "(?:\\d+(?:\\.\\d*)?|\\.\\d*)";
    final String LAG = "(?:[+-](?:\\s*" + NUMBER + "(?:\\s*" + TIME_UNITS + ")?)?)?";
    final String DEPENDENCY = "\\s*" + "\\d*" + "\\s*" + "(?:FS|fs|SS|ss|FF|ff|SF|sf|F|f|S|s)?" + "\\s*" + LAG + "\\s*";
    final String PARTIAL_INPUT_REGEX = "^(?:\\s*|" + DEPENDENCY + "(?:," + DEPENDENCY + ")*)$";

    // allow mid-typing of valid predecessor text
    setAllowed( PARTIAL_INPUT_REGEX );
  }

}
