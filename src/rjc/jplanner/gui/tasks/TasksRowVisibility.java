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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import rjc.jplanner.plan.tasks.Task;
import rjc.table.HashSetInt;
import rjc.table.data.TableData.DataChange;
import rjc.table.signal.IListener;
import rjc.table.undo.IUndoCommand;
import rjc.table.undo.UndoStack;

/*************************************************************************************************/
/******************* Combines summary collapse and manual task row visibility *********************/
/*************************************************************************************************/

final class TasksRowVisibility
{
  private final TasksView      m_view;                // view whose row axis reflects the combined state
  private final TasksViewInput m_input;               // selection and focus adjustments after hiding rows
  private final TasksData      m_data;                // task data and cached outline relationships
  private final Set<Task>      m_collapsedTasks;      // summaries collapsed in this view
  private final Set<Task>      m_manuallyHiddenTasks; // rows hidden independently of summary collapse
  private final IListener      m_historyListener;     // refreshes geometry after complete history operations
  private UndoStack            m_undoStack;           // currently observed history

  /**************************************** constructor ******************************************/
  TasksRowVisibility( TasksView view, TasksViewInput input )
  {
    m_view = view;
    m_input = input;
    m_data = (TasksData) view.getData();

    // history retains deleted task objects for undo; weak sets release them when history does
    // task identity keeps display state attached to the same objects when their row numbers change
    m_collapsedTasks = Collections.newSetFromMap( new WeakHashMap<>() );
    m_manuallyHiddenTasks = Collections.newSetFromMap( new WeakHashMap<>() );
    var tasks = m_data.getTasks();
    for ( int row = 0; row < tasks.size(); row++ )
      if ( view.getRowsAxis().getNominalSize( row ) < 0 )
        m_manuallyHiddenTasks.add( tasks.get( row ) );

    // signal registries retain their listeners, so listeners must not retain the view
    var weakVisibility = new WeakReference<>( this );
    m_data.addListener( ( sender, message ) ->
    {
      var visibility = weakVisibility.get();
      // row-count notifications occur before insertion/restoration finishes updating axis geometry
      if ( visibility != null && message[0] == DataChange.WHOLE_TABLE )
        visibility.updateVisibility();
    } );
    m_historyListener = ( sender, message ) ->
    {
      var visibility = weakVisibility.get();
      if ( visibility != null )
        visibility.updateVisibility();
    };
    observeUndoStack( view.getUndoStack() );
  }

  /************************************** observeUndoStack ***************************************/
  void observeUndoStack( UndoStack undostack )
  {
    if ( m_undoStack != null )
      m_undoStack.removeListener( m_historyListener );
    m_undoStack = undostack;
    m_undoStack.addListener( m_historyListener );
  }

  /******************************************** reset ********************************************/
  void reset()
  {
    m_collapsedTasks.clear();
    m_manuallyHiddenTasks.clear();
  }

  /**************************************** isCollapsed ******************************************/
  boolean isCollapsed( Task task )
  {
    return m_collapsedTasks.contains( task );
  }

  /**************************************** setCollapsed *****************************************/
  void setCollapsed( Task task, boolean collapsed )
  {
    int row = m_data.getTasks().indexOf( task );
    if ( row <= 0 || !m_data.isSummary( row ) )
      return;
    if ( collapsed )
      m_collapsedTasks.add( task );
    else
      m_collapsedTasks.remove( task );
    // collapse is display state: no undo entry, clean-state change, or model notification
    updateVisibility();
    m_view.redraw();
  }

  /****************************************** setHidden ******************************************/
  boolean setHidden( HashSetInt viewRows, boolean hidden )
  {
    var tasks = m_data.getTasks();
    var changed = new ArrayList<Task>();
    for ( int viewRow : viewRows.toArray() )
    {
      int row = m_view.getRowsAxis().getDataIndex( viewRow );
      if ( row >= 0 && row < tasks.size() && m_manuallyHiddenTasks.contains( tasks.get( row ) ) != hidden )
        changed.add( tasks.get( row ) );
    }
    return m_view.getUndoStack().push( new HiddenCommand( changed, hidden ) );
  }

  /*************************************** updateVisibility **************************************/
  void updateVisibility()
  {
    var tasks = m_data.getTasks();
    var hierarchy = m_data.getHierarchy();
    var rows = m_view.getRowsAxis();
    var hide = new HashSetInt();
    var show = new HashSetInt();
    var collapsedRows = new BitSet( tasks.size() );

    // subtree intervals include interior blanks; trailing blanks remain outside the group
    int collapsedEnd = 0;
    for ( int row = 0; row < tasks.size(); row++ )
    {
      Task task = tasks.get( row );
      boolean collapsed = row > 0 && row <= collapsedEnd;
      collapsedRows.set( row, collapsed );
      boolean hidden = collapsed || m_manuallyHiddenTasks.contains( task );
      if ( hidden != ( rows.getNominalSize( row ) < 0 ) )
        ( hidden ? hide : show ).add( row );
      if ( row > 0 && m_collapsedTasks.contains( task ) && hierarchy.hasChildren( row ) )
        collapsedEnd = Math.max( collapsedEnd, hierarchy.getSubtreeEndIndex( row ) );
    }

    // axis hide/unhide preserves nominal heights; never use default heights to restore rows
    rows.hide( hide );
    rows.unhide( show );
    m_input.updateSelectionAfterHiding( hierarchy, collapsedRows );
    if ( hide.isEmpty() && show.isEmpty() )
      return;
    m_view.updateLayout();
    m_view.getMouseCell().revalidatePosition();
    m_view.redraw();
  }

  // manual hiding remains undoable independently of the current collapsed summaries
  private final class HiddenCommand implements IUndoCommand
  {
    private final List<Task> m_tasks;  // same task objects whose manual hidden state changed
    private final boolean    m_hidden; // manual hidden state after the command

    /**************************************** constructor ****************************************/
    HiddenCommand( List<Task> tasks, boolean hidden )
    {
      m_tasks = List.copyOf( tasks );
      m_hidden = hidden;
      apply( hidden );
    }

    /******************************************* apply *******************************************/
    private void apply( boolean hidden )
    {
      // changed rows all had the opposite manual state, so replay restores it exactly
      if ( hidden )
        m_manuallyHiddenTasks.addAll( m_tasks );
      else
        m_manuallyHiddenTasks.removeAll( m_tasks );
      updateVisibility();
    }

    /******************************************** undo *******************************************/
    @Override
    public void undo()
    {
      apply( !m_hidden );
    }

    /******************************************** redo *******************************************/
    @Override
    public void redo()
    {
      apply( m_hidden );
    }

    /****************************************** isValid ******************************************/
    @Override
    public boolean isValid()
    {
      return !m_tasks.isEmpty();
    }

    /******************************************** text *******************************************/
    @Override
    public String text()
    {
      return ( m_hidden ? "Hide " : "Unhide " ) + m_tasks.size() + " task rows";
    }
  }
}
