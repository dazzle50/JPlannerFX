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

import javafx.application.Platform;
import javafx.geometry.Pos;
import rjc.jplanner.plan.Day;
import rjc.jplanner.plan.Day.FIELD;
import rjc.jplanner.plan.Days;
import rjc.table.data.TableData;
import rjc.table.view.Colours;
import rjc.table.view.cell.CellVisual;

/*************************************************************************************************/
/**************************** Table data source for showing day-types ****************************/
/*************************************************************************************************/

public class DaysData extends TableData
{
  private Days       m_days;           // array of day-types to be shown on table
  private CellVisual m_disabledVisual; // cell visuals for disabled cells

  /**************************************** constructor ******************************************/
  public DaysData( Days days )
  {
    // construct data model
    m_days = days;
    setRowCount( m_days.size() );
    setColumnCount( calculateColumnCount() );

    // visual for disabled cells
    m_disabledVisual = new CellVisual();
    m_disabledVisual.cellBackground = Colours.CELL_DISABLED_BACKGROUND;
    m_disabledVisual.textPaint = null;
  }

  /************************************ calculateColumnCount *************************************/
  private int calculateColumnCount()
  {
    // if max periods not yet determined, check each day-type to find most
    int maxPeriods = -1;
    for ( Day day : m_days )
      if ( day.getWorkPeriods().size() > maxPeriods )
        maxPeriods = day.getWorkPeriods().size();

    // return calculated column count
    return Day.FIELD.Start.ordinal() + 2 * maxPeriods;
  }

  /****************************************** getValue *******************************************/
  @Override
  public Object getValue( int dataColumn, int dataRow )
  {
    // return blank for corner cell
    if ( dataRow == HEADER && dataColumn == HEADER )
      return null;

    // return column header
    if ( dataRow == HEADER )
    {
      // return start/end and period number
      if ( dataColumn >= FIELD.Start.ordinal() )
      {
        int count = ( dataColumn - FIELD.Start.ordinal() ) / 2;
        return FIELD.values()[dataColumn - count * 2] + " " + ( count + 1 );
      }

      // otherwise return field name
      return FIELD.values()[dataColumn];
    }

    // return row header
    if ( dataColumn == HEADER )
      return dataRow + 1;

    // otherwise return value from day-types array
    return m_days.get( dataRow ).getValue( dataColumn );
  }

  /***************************************** getVisual *******************************************/
  @Override
  public CellVisual getVisual( int dataColumn, int dataRow )
  {
    // return disabled-cells-visual for period starts/ends beyond those defined
    if ( dataRow > HEADER )
      if ( m_days.get( dataRow ).isBlank( dataColumn ) )
        return m_disabledVisual;

    // otherwise return default cell visuals
    var visual = super.getVisual( dataColumn, dataRow );
    if ( dataColumn == Day.FIELD.Name.ordinal() )
      visual.textAlignment = Pos.CENTER_LEFT;
    else
      visual.textAlignment = Pos.CENTER;

    return visual;
  }

  /****************************************** setValue *******************************************/
  @Override
  protected String setValue( int dataColumn, int dataRow, Object newValue, Boolean commit )
  {
    // after committing new work-periods recalculate table column count
    if ( commit && dataColumn == Day.FIELD.Periods.ordinal() )
      Platform.runLater( () -> setColumnCount( calculateColumnCount() ) );

    // test if value can/could be set
    return m_days.get( dataRow ).setValue( dataColumn, newValue, commit );
  }

}
