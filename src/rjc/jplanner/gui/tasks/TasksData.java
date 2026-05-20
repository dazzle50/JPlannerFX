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

package rjc.jplanner.gui.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.geometry.Pos;
import rjc.jplanner.plan.tasks.Task;
import rjc.jplanner.plan.tasks.Task.FIELD;
import rjc.jplanner.plan.tasks.Tasks;
import rjc.table.data.IDataInsertDeleteRows;
import rjc.table.data.IDataSwapRows;
import rjc.table.data.TableData;
import rjc.table.demo.edit.EditableTableRow;
import rjc.table.view.Colours;
import rjc.table.view.cell.CellVisual;

/*************************************************************************************************/
/*************************** Table data source for showing plan tasks ****************************/
/*************************************************************************************************/

public class TasksData extends TableData implements IDataSwapRows, IDataInsertDeleteRows
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
  protected String setValue( int dataColumn, int dataRow, Object newValue, boolean commit )
  {
    // if committing title or type, then signal row changed
    if ( commit && ( dataColumn == FIELD.Title.ordinal() || dataColumn == FIELD.Type.ordinal() ) )
      signalRowChanged( dataRow );

    // test if value can/could be set
    return m_tasks.setValue( dataRow, dataColumn, newValue, commit );
  }

  /****************************************** swapRows *******************************************/
  /**
   * IDataSwapRows - Swaps two rows in the data model to support row reordering/sorting.
   *
   * @param row1  data-based index of the first row
   * @param row2  data-based index of the second row
   * @return {@code true} always, as the swap cannot fail
   */
  @Override
  public boolean swapRows( int row1, int row2 )
  {
    Collections.swap( m_tasks, row1, row2 );
    return true;
  }

  /***************************************** insertRows ******************************************/
  /**
   * IDataInsertDeleteRows - Inserts one or more rows at the specified position.
   *
   * @param insertIndex  data-based index at which to insert; must be &gt;= 0
   * @param rowData      non-null list of row data; {@code null} elements receive default content
   * @return {@code true} if at least one row was inserted, {@code false} if the list is empty
   * @throws IllegalArgumentException if {@code insertIndex} is out of range or
   *                                  {@code rowData} is {@code null}
   */
  @Override
  public boolean insertRows( int insertIndex, List<Object> rowData )
  {
    // build the list of rows to insert, substituting defaults for null elements
    var toInsert = new ArrayList<Task>( rowData.size() );
    for ( var data : rowData )
      toInsert.add( data == null ? new Task() : (Task) data );

    // insert the new rows and update row count
    m_tasks.addAll( insertIndex, toInsert );
    setRowCount( getRowCount() + toInsert.size() );

    return true;
  }

  /***************************************** deleteRows ******************************************/
  /**
   * IDataInsertDeleteRows - Deletes a contiguous range of rows from the data model.
   *
   * @param deleteIndex  data-based index of the first row to delete; must be &gt;= 0
   * @param count        number of consecutive rows to delete; must be &gt; 0
   * @return a list of the deleted {@link EditableTableRow} rows, or {@code null} if the
   *         operation failed
   * @throws IllegalArgumentException if {@code deleteIndex} is out of range or
   *                                  {@code count} is not positive
   */
  @Override
  public List<Object> deleteRows( int deleteIndex, int count )
  {
    // snapshot the resources being removed before clearing the sublist
    var deleted = new ArrayList<Object>( m_tasks.subList( deleteIndex, deleteIndex + count ) );
    m_tasks.subList( deleteIndex, deleteIndex + count ).clear();
    setRowCount( getRowCount() - count );
    return deleted;
  }

}
