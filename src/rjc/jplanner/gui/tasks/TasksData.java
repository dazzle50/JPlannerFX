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
import rjc.jplanner.plan.tasks.Predecessors;
import rjc.jplanner.plan.tasks.Task;
import rjc.jplanner.plan.tasks.Task.FIELD;
import rjc.jplanner.plan.tasks.Tasks;
import rjc.table.data.IDataInsertDeleteRows;
import rjc.table.data.IDataSwapRows;
import rjc.table.data.TableData;
import rjc.table.view.Colours;
import rjc.table.view.cell.CellVisual;

/*************************************************************************************************/
/*************************** Table data source for showing plan tasks ****************************/
/*************************************************************************************************/

public class TasksData extends TableData implements IDataSwapRows, IDataInsertDeleteRows
{
  private Tasks      m_tasks;          // array of tasks to be shown on table
  private CellVisual m_disabledVisual; // cell visuals for disabled cells

  private record TaskPredecessors( Task task, Predecessors predecessors )
  {
  }

  private record TasksPredChanges( List<Task> tasks, List<TaskPredecessors> changes )
  {
  }

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
   * Inserts tasks and restores their historic predecessor dependencies.
   *
   * @param insertIndex  the data-based index at which to insert the tasks
   * @param rowData      a non-null list containing a {@code TasksPredChanges} object as its first element
   * @return {@code true} if the tasks and dependencies were successfully restored
   * @throws IllegalArgumentException if {@code rowData} is empty, does not contain a {@code TasksPredChanges} 
   *                                  object as its first element, or if {@code insertIndex} is out of bounds
   */
  @Override
  public boolean insertRows( int insertIndex, List<Object> rowData )
  {

    // check rowData first element is a TasksPredChanges object, if so, extract the tasks and predecessor changes
    if ( rowData.get( 0 ) instanceof TasksPredChanges tasksPredChanges )
    {
      // insert the new tasks and update row count
      m_tasks.addAll( insertIndex, tasksPredChanges.tasks() );
      setRowCount( getRowCount() + tasksPredChanges.tasks().size() );

      // loop through the predecessor changes and restore the predecessors for each task
      for ( TaskPredecessors change : tasksPredChanges.changes() )
        change.task().setValue( FIELD.Predecessors.ordinal(), change.predecessors(), true );

      return true;
    }

    // otherwise throw an exception, as the rowData is not in the expected format
    throw new IllegalArgumentException( "rowData must contain a TasksPredChanges object as its first element" );
  }

  /***************************************** deleteRows ******************************************/
  /**
   * Deletes a contiguous range of tasks and updates dependent predecessors.
   *
   * @param deleteIndex  the data-based index of the first task to delete
   * @param count        the number of consecutive tasks to delete
   * @return a list of size {@code count} containing a {@code TasksPredChanges} object as its first element followed by padding nulls, used for undo operations
   * @throws IllegalArgumentException if {@code deleteIndex} or {@code count} are out of valid bounds
   */
  @Override
  public List<Object> deleteRows( int deleteIndex, int count )
  {
    // snapshot the tasks being removed before clearing the sublist
    var removedTasks = new ArrayList<Task>( m_tasks.subList( deleteIndex, deleteIndex + count ) );
    m_tasks.subList( deleteIndex, deleteIndex + count ).clear();
    setRowCount( getRowCount() - count );

    // loop through remaining tasks and remove any dependencies that reference the deleted tasks
    var predecessorChanges = new ArrayList<TaskPredecessors>();
    for ( Task task : m_tasks )
    {
      Predecessors beforeRemove = task.getPredecessors();
      if ( beforeRemove == null )
        continue;

      // altered predecessors will have shorter length than before
      Predecessors afterRemove = Predecessors.withoutTasks( beforeRemove, removedTasks );
      if ( afterRemove == null || beforeRemove.size() != afterRemove.size() )
      {
        task.setValue( FIELD.Predecessors.ordinal(), afterRemove, true );
        predecessorChanges.add( new TaskPredecessors( task, beforeRemove ) );
      }
    }

    // create undo data for the deleted tasks and any predecessor changes, add nulls to list to match size of deleted rows
    var undoData = new ArrayList<Object>();
    undoData.add( new TasksPredChanges( removedTasks, predecessorChanges ) );
    while ( undoData.size() < count )
      undoData.add( null );

    return undoData;
  }

}
