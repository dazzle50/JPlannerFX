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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
  private static final double            TITLE_INDENT_WIDTH = 16.0;            // logical pixels per task indentation level

  private final Tasks                    m_tasks;                              // tasks displayed by this data source
  private final CellVisual               m_disabledVisual;                     // visual for disabled cells
  private final CellVisual               m_readonlyVisual;                     // visual for read-only cells
  private final Map<Integer, CellVisual> m_titleVisuals     = new HashMap<>(); // title visuals by indentation level

  /**************************************** constructor ******************************************/
  public TasksData( Tasks tasks )
  {
    // construct data model
    m_tasks = tasks;
    setRowCount( m_tasks.size() );
    setColumnCount( FIELD.MAX.ordinal() );

    // visual for disabled & read-only cells
    m_disabledVisual = new CellVisual();
    m_disabledVisual.cellBackground = Colours.CELL_DISABLED_BACKGROUND;
    m_disabledVisual.textPaint = null;
    m_readonlyVisual = new CellVisual();
    m_readonlyVisual.cellBackground = Colours.CELL_DISABLED_BACKGROUND;
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
    // return read-only visual for special row 0 'overall project'
    if ( dataRow == 0 )
      return m_readonlyVisual;

    // keep title spacing separate from the shared default visual so other cells cannot inherit it
    if ( dataRow > HEADER && dataColumn == FIELD.Title.ordinal() )
    {
      var task = m_tasks.get( dataRow );
      int indent = task.isBlank() ? 0 : task.getIndent();
      return m_titleVisuals.computeIfAbsent( indent, level ->
      {
        var visual = new CellVisual();
        var insets = visual.textInsets;
        visual.textAlignment = Pos.CENTER_LEFT;
        visual.textInsets = new Insets( insets.getTop(), insets.getRight(), insets.getBottom(),
            insets.getLeft() + level * TITLE_INDENT_WIDTH );
        return visual;
      } );
    }

    // return disabled visual for non-initials field if initials is blank
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
        case Comment, Predecessors, Resources -> Pos.CENTER_LEFT;
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
    if ( dataColumn == FIELD.Title.ordinal() )
    {
      if ( commit )
        return "Task titles require a title-and-indentation command";
      m_tasks.proposedTitle( dataRow, newValue );
      return null;
    }

    // notify after task-type changes; title commands apply their own complete-state notification
    String problem = m_tasks.setValue( dataRow, dataColumn, newValue, commit );
    if ( problem == null && commit && dataColumn == FIELD.Type.ordinal() )
      signalRowChanged( dataRow );

    return problem;
  }

  /****************************************** swapRows *******************************************/
  /**
   * IDataSwapRows - Swaps two rows in the data model to support row reordering/sorting.
   *
   * @param row1  data-based index of the first row
   * @param row2  data-based index of the second row
   * @return never returns while task reordering is unsupported
   * @throws UnsupportedOperationException because per-swap rejection cannot validate a complete reorder
   */
  @Override
  public boolean swapRows( int row1, int row2 )
  {
    throw new UnsupportedOperationException( "Task row reordering is temporarily unavailable" );
  }

  /*************************************** checkInsertRows ***************************************/
  public void checkInsertRows( int[] dataRows )
  {
    if ( dataRows == null )
      throw new IllegalArgumentException( "Insertion targets must not be null" );

    for ( int row : dataRows )
      if ( row <= 0 || row > m_tasks.size() )
        throw new IllegalArgumentException( "Cannot insert before project task 0 or outside the task list" );
    m_tasks.buildHierarchy();
  }

  /************************************** checkRowsDeletable *************************************/
  @Override
  public String checkRowsDeletable( int[] dataRows )
  {
    if ( dataRows == null )
      return "Deletion targets must not be null";

    for ( int row : dataRows )
      if ( row <= 0 || row >= m_tasks.size() || !m_tasks.get( row ).isBlank() )
        return "Use whole-group deletion for named tasks; project task 0 cannot be deleted";
    return null;
  }

  /***************************************** insertRows ******************************************/
  /**
   * Inserts blank tasks or restores the original blank objects retained by insertion history.
   *
   * @param insertIndex  the data-based index at which to insert the tasks
   * @param rowData      null entries for new blank tasks, or the original blank task objects for restoration
   * @return {@code true} if the blank tasks were successfully inserted
   * @throws IllegalArgumentException if data is invalid, contains named tasks, or insertion would precede task 0
   */
  @Override
  public boolean insertRows( int insertIndex, List<Object> rowData )
  {
    checkInsertRows( new int[] { insertIndex } );
    if ( rowData == null || rowData.isEmpty() )
      throw new IllegalArgumentException( "Expected insertion data" );

    // validate the whole payload before insertion; history restores the same blank objects
    boolean defaults = rowData.getFirst() == null;
    var blankTasks = new ArrayList<Task>( rowData.size() );
    for ( Object value : rowData )
    {
      if ( defaults && value == null )
        blankTasks.add( new Task() );
      else if ( !defaults && value instanceof Task task && task.isBlank() )
        blankTasks.add( task );
      else
        throw new IllegalArgumentException( "Expected all null entries or all blank task objects" );
    }

    m_tasks.addAll( insertIndex, blankTasks );
    setRowCount( m_tasks.size() );
    return true;
  }

  /***************************************** deleteRows ******************************************/
  /**
   * Deletes an exact contiguous range of blank rows, retaining objects for insertion redo.
   * Named tasks require the separate whole-group command; this method never expands a range.
   *
   * @param deleteIndex  the data-based index of the first task to delete
   * @param count        the number of consecutive tasks to delete
   * @return the original blank task objects in row order, retained for insertion redo
   * @throws IllegalArgumentException if {@code deleteIndex} or {@code count} are out of valid bounds
   */
  @Override
  public List<Object> deleteRows( int deleteIndex, int count )
  {
    if ( count <= 0 || deleteIndex <= 0 || deleteIndex > m_tasks.size() - count )
      throw new IllegalArgumentException( "Invalid blank-row deletion range" );

    m_tasks.buildHierarchy();
    for ( int row = deleteIndex; row < deleteIndex + count; row++ )
      if ( !m_tasks.get( row ).isBlank() )
        throw new IllegalArgumentException( "Named tasks require whole-group deletion" );

    // retain only blank objects; supported dependencies cannot target blank rows
    var removedTasks = new ArrayList<Object>( m_tasks.subList( deleteIndex, deleteIndex + count ) );
    m_tasks.subList( deleteIndex, deleteIndex + count ).clear();
    setRowCount( m_tasks.size() );
    return removedTasks;
  }

}
