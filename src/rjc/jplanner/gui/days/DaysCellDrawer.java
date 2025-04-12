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

import javafx.geometry.Pos;
import javafx.scene.paint.Paint;
import rjc.jplanner.plan.Day;
import rjc.table.data.types.Time;
import rjc.table.view.Colours;
import rjc.table.view.cell.CellDrawer;

/*************************************************************************************************/
/*************************** Draws table-view cells for plan day-types ***************************/
/*************************************************************************************************/

public class DaysCellDrawer extends CellDrawer
{

  /************************************ getTextAlignment *************************************/
  @Override
  protected Pos getTextAlignment()
  {
    // return left alignment for Name column
    if ( getDataColumn() == Day.FIELD.Name.ordinal() )
      return Pos.CENTER_LEFT;

    // otherwise centre alignment
    return Pos.CENTER;
  }

  /********************************** getBackgroundPaintDefault **********************************/
  @Override
  protected Paint getBackgroundPaintDefault()
  {
    // return disabled cell background for Work if no periods
    int col = getDataColumn();
    int row = getDataRow();
    int periods = (int) view.getData().getValue( Day.FIELD.Periods.ordinal(), row );
    if ( periods == 0 && col == Day.FIELD.Work.ordinal() )
      return Colours.CELL_DISABLED_BACKGROUND;

    // return disabled cell background for unused periods
    if ( col >= 2 * periods + Day.FIELD.Start.ordinal() )
      return Colours.CELL_DISABLED_BACKGROUND;

    // default table cell background
    return Colours.CELL_DEFAULT_BACKGROUND;
  }

  /****************************************** getText ********************************************/
  @Override
  protected String getText()
  {
    // return cell value as string
    Object value = getData();
    if ( value instanceof Time time )
      return time.toStringShort();

    return value == null ? null : value.toString();
  }
}
