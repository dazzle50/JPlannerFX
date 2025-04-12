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

package rjc.jplanner.gui.calendars;

import javafx.geometry.Pos;
import javafx.scene.paint.Paint;
import rjc.jplanner.plan.Calendar;
import rjc.table.view.Colours;
import rjc.table.view.axis.TableAxis;
import rjc.table.view.cell.CellDrawer;

/*************************************************************************************************/
/*************************** Draws table-view cells for plan calendars ***************************/
/*************************************************************************************************/

public class CalendarsCellDrawer extends CellDrawer
{

  /************************************ getTextAlignment *************************************/
  @Override
  protected Pos getTextAlignment()
  {
    // return centre alignment for headers
    if ( viewColumn == TableAxis.HEADER || viewRow == TableAxis.HEADER )
      return Pos.CENTER;

    // otherwise centre-left alignment
    return Pos.CENTER_LEFT;
  }

  /********************************** getBackgroundPaintDefault **********************************/
  @Override
  protected Paint getBackgroundPaintDefault()
  {
    // return disabled cell background for empty normal cells
    int col = getDataColumn();
    int row = getDataRow();
    int cycle = (int) view.getData().getValue( col, Calendar.FIELD.Cycle.ordinal() );
    if ( row >= cycle + Calendar.FIELD.Normal.ordinal() )
      return Colours.CELL_DISABLED_BACKGROUND;

    // otherwise default table cell background
    return Colours.CELL_DEFAULT_BACKGROUND;
  }

}
