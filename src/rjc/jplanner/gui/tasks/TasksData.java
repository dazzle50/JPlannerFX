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

package rjc.jplanner.gui.tasks;

import javafx.geometry.Pos;
import rjc.jplanner.plan.Task.FIELD;
import rjc.jplanner.plan.Tasks;
import rjc.table.data.TableData;
import rjc.table.view.Colours;
import rjc.table.view.cell.CellVisual;

/*************************************************************************************************/
/*************************** Table data source for showing plan tasks ****************************/
/*************************************************************************************************/

public class TasksData extends TableData
{
  private Tasks      m_tasks;          // array of tasks to be shown on table
  private CellVisual m_disabledVisual; // cell visuals for disabled cells

  /**************************************** constructor ******************************************/
  public TasksData( Tasks tasks )
  {
    // construct data model
    m_tasks = tasks;
    setRowCount( m_tasks.size() );
    setColumnCount( FIELD.MAX.ordinal() );

    // visual for disabled cells
    m_disabledVisual = new CellVisual();
    m_disabledVisual.cellBackground = Colours.CELL_DISABLED_BACKGROUND;
    m_disabledVisual.textPaint = null;
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
      return FIELD.values()[dataColumn];

    // return row header
    if ( dataColumn == HEADER )
      return dataRow;

    // otherwise return value from tasks array
    return m_tasks.get( dataRow ).getValue( dataColumn );
  }

  /***************************************** getVisual *******************************************/
  @Override
  public CellVisual getVisual( int dataColumn, int dataRow )
  {
    // return disabled-cells-visual for non-initials field if initials is blank
    if ( dataRow > HEADER && dataColumn > FIELD.Title.ordinal() )
      if ( m_tasks.get( dataRow ).isBlank() )
        return m_disabledVisual;

    var visual = super.getVisual( dataColumn, dataRow );
    visual.textAlignment = Pos.CENTER;

    if ( dataRow > HEADER && dataColumn > HEADER )
    {
      // different fields have different text alignments
      visual.textAlignment = switch ( FIELD.values()[dataColumn] )
      {
        case Title, Comment, Predecessors, Resources -> Pos.CENTER_LEFT;
        case Work -> Pos.CENTER_RIGHT;
        default -> Pos.CENTER;
      };

      // different task types have different fields read-only (shown by cell background colour)
      var type = m_tasks.get( dataRow ).getTaskType();
      visual.cellBackground = switch ( FIELD.values()[dataColumn] )
      {
        case Duration -> type.durationBackground();
        case Start -> type.startBackground();
        case End -> type.endBackground();
        case Work -> type.workBackground();
        default -> Colours.CELL_DEFAULT_BACKGROUND;
      };
    }

    return visual;
  }

  /****************************************** setValue *******************************************/
  @Override
  protected String setValue( int dataColumn, int dataRow, Object newValue, Boolean commit )
  {
    // test if value can/could be set
    return m_tasks.get( dataRow ).setValue( dataColumn, newValue, commit );
  }

}
