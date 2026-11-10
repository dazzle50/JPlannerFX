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

package rjc.jplanner.plan.tasks;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import rjc.jplanner.Main;
import rjc.jplanner.plan.Plan;
import rjc.table.Utils;

/*************************************************************************************************/
/**************************** Holds the complete list of plan tasks ******************************/
/*************************************************************************************************/

public class Tasks extends ArrayList<Task>
{
  private static final long   serialVersionUID = Main.VERSION.hashCode();

  private WeakReference<Plan> m_weakPlan;

  public record TitleState( String title, int indent )
  {
  }

  private record PredecessorChange( Task task, Predecessors before, Predecessors after )
  {
  }

  public static final class DeletionState
  {
    private final int[]                   m_dataRows;     // original deleted model positions
    private final List<Task>              m_removed;      // original task objects in position order
    private final List<PredecessorChange> m_predecessors; // changed surviving dependency values

    /************************************** constructor ****************************************/
    private DeletionState( int[] dataRows, List<Task> removed, List<PredecessorChange> predecessors )
    {
      m_dataRows = dataRows;
      m_removed = List.copyOf( removed );
      m_predecessors = List.copyOf( predecessors );
    }

    /************************************** getDataRows ****************************************/
    public int[] getDataRows()
    {
      return m_dataRows.clone();
    }
  }

  /**************************************** constructor ******************************************/
  public Tasks( Plan plan )
  {
    // hold plan weakly so does not prevent garbage collection of plan
    m_weakPlan = new WeakReference<>( plan );
  }

  /****************************************** initialise *****************************************/
  public void initialise()
  {
    // initialise list with default tasks
    clear();
    for ( int count = 0; count <= 10; count++ )
      add( new Task() );

    // setup special task 0 (overall project summary)
    get( 0 ).setValue( Task.FIELD.Title.ordinal(), "[OVERALL_PROJECT]", true );
  }

  /*************************************** getNotNullCount ***************************************/
  public int getNotNullCount()
  {
    // return number of not-null tasks in plan (skipping special task 0)
    int count = 0;
    for ( int id = 1; id < size(); id++ )
      if ( !get( id ).isBlank() )
        count++;

    return count;
  }

  /*************************************** buildHierarchy ***************************************/
  public TaskHierarchy buildHierarchy()
  {
    // build once per operation or refresh; rebuild after task order, titles, or indentation change
    return new TaskHierarchy( this );
  }

  /*************************************** proposedIndents ***************************************/
  public int[] proposedIndents( int[] dataRows, int direction )
  {
    // validate every supplied model index before filtering blanks or expanding descendants
    if ( direction != 1 && direction != -1 )
      throw new IllegalArgumentException( "Indentation direction must be +1 or -1" );
    if ( dataRows == null )
      throw new IllegalArgumentException( "Selected task rows must not be null" );
    for ( int row : dataRows )
    {
      if ( row == 0 )
        throw new IllegalArgumentException( "The project summary cannot be indented or outdented" );
      if ( row < 0 || row >= size() )
        throw new IllegalArgumentException( "Invalid task row: " + row );
    }

    var hierarchy = buildHierarchy();
    int[] proposal = new int[size()];
    boolean[] selected = new boolean[size()];
    for ( int row = 0; row < size(); row++ )
      proposal[row] = get( row ).getIndent();
    for ( int row : dataRows )
      selected[row] = !get( row ).isBlank();

    // original subtree boundaries prevent overlaps or newly acquired children changing tasks twice
    int end = 0;
    int previousIndent = -1;
    for ( int row = 1; row < size(); row++ )
    {
      if ( get( row ).isBlank() )
        continue;
      if ( selected[row] )
        end = Math.max( end, hierarchy.getSubtreeEndIndex( row ) );
      if ( row <= end )
      {
        proposal[row] += direction;
        // the original outline is valid, so these failures describe the requested user move
        if ( proposal[row] < 0 )
          throw new IllegalStateException( "Task " + row + " is already at the top level." );
        if ( proposal[row] > previousIndent + 1 )
          throw new IllegalStateException(
              "Task " + row + " needs a task above it at the same level before it can be indented" );
      }
      previousIndent = proposal[row];
    }

    // validate the complete candidate without changing live task inputs
    new TaskHierarchy( this, proposal );
    return proposal;
  }

  /***************************************** setIndents ******************************************/
  public void setIndents( int[] indents )
  {
    // loading may supply rows in any order, so validate the complete outline before changing tasks
    if ( indents.length != size() )
      throw new IllegalArgumentException( "Expected " + size() + " task indentation values" );
    for ( int index = 0; index < indents.length; index++ )
      if ( indents[index] < 0 )
        throw new IllegalArgumentException( "Task " + index + " indentation cannot be negative: " + indents[index] );

    new TaskHierarchy( this, indents );
    for ( int index = 0; index < indents.length; index++ )
      get( index ).setIndent( indents[index] );
  }

  /*************************************** proposedTitle *****************************************/
  public TitleState proposedTitle( int dataRow, Object value )
  {
    if ( dataRow <= 0 || dataRow >= size() )
      throw new IllegalArgumentException( "Invalid editable task row: " + dataRow );
    var hierarchy = buildHierarchy();
    String title = Utils.clean( value == null ? null : value.toString() );
    if ( title.isEmpty() )
      title = null;
    Task task = get( dataRow );
    if ( title == null && !task.isBlank() )
      throw new IllegalArgumentException( "Title length must be at least 1 character" );

    int indent = task.getIndent();
    if ( task.isBlank() && title != null )
    {
      // task 0 is the project root, not a preceding editable summary
      int previous = dataRow - 1;
      while ( previous > 0 && get( previous ).isBlank() )
        previous--;
      indent = previous == 0 ? 0 : get( previous ).getIndent() + ( hierarchy.hasChildren( previous ) ? 1 : 0 );
    }
    var state = new TitleState( title, indent );
    validateTitle( dataRow, state );
    return state;
  }

  /**************************************** validateTitle ****************************************/
  private void validateTitle( int dataRow, TitleState state )
  {
    if ( dataRow <= 0 || dataRow >= size() || state == null || state.indent() < 0 )
      throw new IllegalArgumentException( "Invalid task title state at row " + dataRow );
    int[] indents = new int[size()];
    for ( int row = 0; row < size(); row++ )
      indents[row] = row == dataRow ? state.indent() : get( row ).getIndent();
    new TaskHierarchy( this, indents, dataRow, state.title() == null );
  }

  /****************************************** setTitle *******************************************/
  public void setTitle( int dataRow, TitleState state )
  {
    // exact restoration permits undoing creation; user clearing restrictions belong to the proposal
    validateTitle( dataRow, state );
    get( dataRow ).setValue( Task.FIELD.Title.ordinal(), state.title(), true );
    get( dataRow ).setIndent( state.indent() );
  }

  /************************************** proposedDeleteRows *************************************/
  public int[] proposedDeleteRows( int[] dataRows )
  {
    if ( dataRows == null )
      throw new IllegalArgumentException( "Selected task rows must not be null" );
    for ( int row : dataRows )
      if ( row <= 0 || row >= size() )
        throw new IllegalArgumentException( "Invalid deletable task row: " + row );
    var hierarchy = buildHierarchy();
    boolean[] selected = new boolean[size()];
    for ( int row : dataRows )
      selected[row] = true;
    var survivors = new ArrayList<Task>( size() );
    int[] removed = new int[size()];
    int count = 0;
    int end = -1;
    for ( int row = 0; row < size(); row++ )
    {
      if ( selected[row] )
        end = Math.max( end, Math.max( row, hierarchy.getSubtreeEndIndex( row ) ) );
      // interior blanks belong to a deleted group; trailing blanks need explicit selection
      if ( row <= end )
        removed[count++] = row;
      else
        survivors.add( get( row ) );
    }
    new TaskHierarchy( survivors );
    return Arrays.copyOf( removed, count );
  }

  /*************************************** prepareDeletion ***************************************/
  public DeletionState prepareDeletion( int[] dataRows )
  {
    int[] rows = proposedDeleteRows( dataRows );
    var removed = new ArrayList<Task>( rows.length );
    for ( int row : rows )
      removed.add( get( row ) );
    var removedSet = new HashSet<>( removed );
    var changes = new ArrayList<PredecessorChange>();
    for ( Task task : this )
      if ( !removedSet.contains( task ) )
      {
        var before = task.getPredecessors();
        var after = Predecessors.withoutTasks( before, removedSet );
        if ( before != after )
          changes.add( new PredecessorChange( task, before, after ) );
      }
    return new DeletionState( rows, removed, changes );
  }

  /*************************************** deletionResult ****************************************/
  private List<Task> deletionResult( DeletionState state, boolean restore )
  {
    if ( state == null )
      throw new IllegalArgumentException( "Deletion state must not be null" );
    buildHierarchy();
    int count = state.m_dataRows.length;
    int total = restore ? size() + count : size();
    for ( int row : state.m_dataRows )
      if ( row <= 0 || row >= total )
        throw new IllegalStateException( "Deleted task positions no longer fit the plan" );
    var result = new ArrayList<Task>( restore ? total : Math.max( 0, size() - count ) );
    int saved = 0;
    int current = 0;
    for ( int row = 0; row < total; row++ )
      if ( saved < count && row == state.m_dataRows[saved] )
      {
        if ( restore )
          result.add( state.m_removed.get( saved ) );
        else if ( get( row ) != state.m_removed.get( saved ) )
          throw new IllegalStateException( "Deleted task identity changed at row " + row );
        saved++;
      }
      else
        result.add( get( restore ? current++ : row ) );
    if ( saved != count )
      throw new IllegalStateException( "Deleted task positions no longer fit the plan" );
    new TaskHierarchy( result );
    var present = new HashSet<>( result );
    for ( var change : state.m_predecessors )
    {
      if ( !present.contains( change.task() )
          || !samePredecessors( change.task().getPredecessors(), restore ? change.after() : change.before() ) )
        throw new IllegalStateException( "Task dependency history no longer matches the plan" );
      var target = restore ? change.before() : change.after();
      if ( target != null && target.hasCircularReference( change.task() ) )
        throw new IllegalStateException( "Cannot restore circular task dependencies" );
    }
    return result;
  }

  /************************************** samePredecessors ***************************************/
  private boolean samePredecessors( Predecessors current, Predecessors expected )
  {
    // scalar predecessor undo reparses equivalent values, so container identity is not sufficient
    if ( current == expected )
      return true;
    if ( current == null || expected == null || current.size() != expected.size() )
      return false;
    // display text omits zero-lag units, so reparsing can change their unit without changing meaning
    for ( int index = 0; index < current.size(); index++ )
    {
      var actual = current.get( index );
      var saved = expected.get( index );
      if ( actual.task != saved.task || actual.type != saved.type || actual.lag100ths != saved.lag100ths
          || ( actual.lag100ths != 0 && actual.lagUnit != saved.lagUnit ) )
        return false;
    }
    return true;
  }

  /************************************** validateDeletion ***************************************/
  public void validateDeletion( DeletionState state, boolean restore )
  {
    deletionResult( state, restore );
  }

  /*************************************** applyDeletion *****************************************/
  public void applyDeletion( DeletionState state, boolean restore )
  {
    var result = deletionResult( state, restore );
    // retain the collection and task objects; restore all rows before restoring their links
    clear();
    addAll( result );
    for ( var change : state.m_predecessors )
    {
      String problem = change.task().setValue( Task.FIELD.Predecessors.ordinal(),
          restore ? change.before() : change.after(), true );
      if ( problem != null )
        throw new IllegalStateException( problem );
    }
  }

  /****************************************** setValue *******************************************/
  public String setValue( int taskIndex, int field, Object newValue, boolean commit )
  {
    if ( newValue != null )
    {
      try
      {
        // validate and parse specific fields if required
        if ( field == Task.FIELD.Predecessors.ordinal() )
          newValue = Predecessors.parse( newValue.toString(), this );
        else if ( field == Task.FIELD.Resources.ordinal() )
          newValue = TaskResources.parse( newValue.toString(), m_weakPlan.get().getResources() );
      }
      catch ( IllegalArgumentException exception )
      {
        // validation failed, return error message
        return exception.getMessage();
      }
    }

    // delegate to task to set value, and return any error message
    return get( taskIndex ).setValue( field, newValue, commit );
  }

}
