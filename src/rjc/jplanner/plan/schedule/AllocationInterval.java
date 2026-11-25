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

package rjc.jplanner.plan.schedule;

import java.util.Objects;

import rjc.jplanner.plan.resources.Resource;
import rjc.jplanner.plan.tasks.Task;
import rjc.table.data.types.DateTime;

/*************************************************************************************************/
/*********************** Immutable handle to one stored allocation interval **********************/
/*************************************************************************************************/

public final class AllocationInterval
{
  private final AllocationStore m_store;    // immutable allocation data retained by this handle
  private final int             m_rowIndex; // row within this store, not a plan row number

  /***************************************** constructor *****************************************/
  AllocationInterval( AllocationStore store, int index )
  {
    // bind this immutable handle to one validated canonical row of the retained store
    m_store = Objects.requireNonNull( store, "store" );
    m_rowIndex = Objects.checkIndex( index, store.size() );
  }

  /****************************************** getStore *******************************************/
  public AllocationStore getStore()
  {
    // return the retained immutable store independently of later Plan replacements
    return m_store;
  }

  /***************************************** getRowIndex *****************************************/
  public int getRowIndex()
  {
    // return this handle's canonical store row, independently of current task or resource positions
    return m_rowIndex;
  }

  /*********************************** getTaskParticipantIndex ***********************************/
  public int getTaskParticipantIndex()
  {
    // return the task participant index captured in this store for the allocation row
    // participant indexes identify entries in this store, independently of current plan rows
    return m_store.getTaskParticipantIndex( m_rowIndex );
  }

  /********************************* getResourceParticipantIndex *********************************/
  public int getResourceParticipantIndex()
  {
    // return the resource participant index captured in this store for the allocation row
    // resource participant zero represents independent Unassigned capacity for this task
    return m_store.getResourceParticipantIndex( m_rowIndex );
  }

  /******************************************* getTask *******************************************/
  public Task getTask()
  {
    // return the original task binding rather than the current occupant of a plan row
    return m_store.getTask( m_rowIndex );
  }

  /***************************************** getResource *****************************************/
  public Resource getResource()
  {
    // return the original resource binding, including resource participant zero for Unassigned
    return m_store.getResource( m_rowIndex );
  }

  /*************************************** getStartMillis ****************************************/
  public long getStartMillis()
  {
    // return the inclusive start as serial milliseconds without constructing a datetime
    return m_store.getStartMillis( m_rowIndex );
  }

  /**************************************** getEndMillis *****************************************/
  public long getEndMillis()
  {
    // return the exclusive end as serial milliseconds without constructing a datetime
    return m_store.getEndMillis( m_rowIndex );
  }

  /****************************************** getStart *******************************************/
  public DateTime getStart()
  {
    // convert the inclusive start to a datetime only when explicitly requested
    // convert only on request; numeric iteration does not create datetime objects
    return DateTime.ofSerialMilliseconds( getStartMillis() );
  }

  /******************************************* getEnd ********************************************/
  public DateTime getEnd()
  {
    // convert the exclusive end to a datetime only when explicitly requested
    return DateTime.ofSerialMilliseconds( getEndMillis() );
  }

  /**************************************** getQuantity ******************************************/
  public double getQuantity()
  {
    // return the constant capacity occupied by this interval through its represented end
    return m_store.getQuantity( m_rowIndex );
  }

  /**************************************** getWorkMillis ****************************************/
  public double getWorkMillis()
  {
    // return measured resource-millisecond credit, independently of the occupied interval length
    // resource effort can be fractional: half a unit for one elapsed millisecond credits 0.5 resource-ms
    // credited work can be less than quantity times occupied milliseconds at completion
    return m_store.getWorkMillis( m_rowIndex );
  }

  /***************************************** getWorkDays *****************************************/
  public double getWorkDays()
  {
    // return measured equivalent resource-days without rereading an edited calendar
    return m_store.getWorkDays( m_rowIndex );
  }

  /******************************************** equals *******************************************/
  @Override
  public boolean equals( Object other )
  {
    // compare logical allocation identity using store identity and canonical row index
    // allocation identity is the stored row, not the handle object or its numeric values alone
    return other instanceof AllocationInterval interval && m_store == interval.m_store
        && m_rowIndex == interval.m_rowIndex;
  }

  /******************************************* hashCode ******************************************/
  @Override
  public int hashCode()
  {
    // hash the same store identity and canonical row used by allocation equality
    return 31 * System.identityHashCode( m_store ) + m_rowIndex;
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // describe this row's participant indexes, dates, occupied quantity and measured credits
    return "AllocationInterval[row=" + m_rowIndex + ", task=" + getTaskParticipantIndex() + ", resource="
        + getResourceParticipantIndex() + ", start=" + getStart() + ", end=" + getEnd() + ", quantity=" + getQuantity()
        + ", workMillis=" + getWorkMillis() + ", workDays=" + getWorkDays() + "]";
  }
}
