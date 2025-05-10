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

package rjc.jplanner.gui.days;

import rjc.table.data.types.Time;
import rjc.table.view.cell.CellDrawer;

/*************************************************************************************************/
/*************************** Draws table-view cells for plan day-types ***************************/
/*************************************************************************************************/

public class DaysCellDrawer extends CellDrawer
{

  /****************************************** getText ********************************************/
  @Override
  protected String getText()
  {
    // return cell value as string
    if ( value instanceof Time time )
      return time.toStringShort();
    if ( value instanceof Double work )
      return String.format( "%.2f", work );

    return value == null ? null : value.toString();
  }
}
