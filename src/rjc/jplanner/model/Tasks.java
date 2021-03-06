/**************************************************************************
 *  Copyright (C) 2018 by Richard Crook                                   *
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

package rjc.jplanner.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import rjc.jplanner.JPlanner;
import rjc.jplanner.XmlLabels;

/*************************************************************************************************/
/**************************** Holds the complete list of plan tasks ******************************/
/*************************************************************************************************/

public class Tasks extends ArrayList<Task>
{
  private static final long serialVersionUID = 1L;

  public class PredecessorsList extends TreeMap<Integer, String>
  {
    private static final long serialVersionUID = 1L;

    public String toString( String start )
    {
      // construct message to indicate change to predecessors
      if ( size() == 0 )
        return "";

      if ( size() == 1 )
        return start + " predecessors for task " + firstKey();

      StringBuilder str = new StringBuilder( start + " predecessors for tasks " );
      for ( int id : keySet() )
      {
        if ( id == lastKey() )
          str.append( " & " );
        if ( id != lastKey() && id != firstKey() )
          str.append( ", " );
        str.append( id );
      }
      return str.toString();
    }
  }

  /****************************************** initialise *****************************************/
  public void initialise()
  {
    // initialise list with default tasks (including special task 0)
    clear();
    for ( int count = 0; count <= 200; count++ )
      add( new Task() );

    setupTaskZero();
  }

  /**************************************** setupTaskZero ****************************************/
  public void setupTaskZero()
  {
    // setup special task 0
    Task task = get( 0 );
    task.setValue( Task.SECTION_TITLE, "PROJECT" );
    task.setValue( Task.SECTION_START, DateTime.now() );
    task.setValue( Task.SECTION_END, DateTime.now() );
    task.setIndent( -1 );
    updateSummaryMarkers();
  }

  /******************************************* loadXML *******************************************/
  public void loadXML( XMLStreamReader xsr ) throws XMLStreamException
  {
    // read any attributes
    for ( int i = 0; i < xsr.getAttributeCount(); i++ )
      switch ( xsr.getAttributeLocalName( i ) )
      {
        default:
          JPlanner.trace( "Unhandled attribute '" + xsr.getAttributeLocalName( i ) + "'" );
          break;
      }

    // read XML task data
    while ( xsr.hasNext() )
    {
      xsr.next();

      // if reached end of task data, return
      if ( xsr.isEndElement() && xsr.getLocalName().equals( XmlLabels.XML_TASK_DATA ) )
      {
        setupTaskZero();
        updateSummaryMarkers();
        return;
      }

      // if element start, load the contents
      if ( xsr.isStartElement() )
        switch ( xsr.getLocalName() )
        {
          case XmlLabels.XML_TASK:
            add( new Task( xsr ) );
            break;
          case XmlLabels.XML_PREDECESSORS:
            loadPredecessors( xsr );
            break;
          default:
            JPlanner.trace( "Unhandled start element '" + xsr.getLocalName() + "'" );
            break;
        }
    }
  }

  /*************************************** loadPredecessors **************************************/
  private void loadPredecessors( XMLStreamReader xsr )
  {
    // initialise variables
    int task = -1;
    String preds = "";

    // read XML predecessors attributes
    for ( int i = 0; i < xsr.getAttributeCount(); i++ )
      switch ( xsr.getAttributeLocalName( i ) )
      {
        case XmlLabels.XML_TASK:
          task = Integer.parseInt( xsr.getAttributeValue( i ) );
          break;
        case XmlLabels.XML_PREDS:
          preds = xsr.getAttributeValue( i );
          break;
        default:
          JPlanner.trace( "Unhandled attribute '" + xsr.getAttributeLocalName( i ) + "'" );
          break;
      }

    // set the task predecessors, remembering array starts from zero but id from one
    get( task ).setValue( Task.SECTION_PRED, new Predecessors( preds ) );
  }

  /******************************************* writeXML ******************************************/
  public void writeXML( XMLStreamWriter xsw ) throws XMLStreamException
  {
    // write tasks data to XML stream
    xsw.writeStartElement( XmlLabels.XML_TASK_DATA );
    for ( Task task : this )
      task.saveToXML( xsw );

    // write predecessors data to XML stream
    for ( Task task : this )
      task.savePredecessorToXML( xsw );

    xsw.writeEndElement(); // XML_TASK_DATA 
  }

  /****************************************** schedule *******************************************/
  public void schedule()
  {
    // first construct set of tasks in correct order
    TreeSet<Task> scheduleList = new TreeSet<Task>();
    for ( Task task : this )
      if ( !task.isNull() )
        scheduleList.add( task );

    // schedule tasks in this order
    scheduleList.forEach( task -> task.schedule() );
  }

  /****************************************** canIndent ******************************************/
  public Set<Integer> canIndent( Set<Integer> rows )
  {
    // return the subset of rows that can be indented
    Set<Integer> can = new HashSet<Integer>();
    for ( int row : rows )
    {
      // cannot indent hidden task 0 or task 1
      if ( row <= 1 )
        continue;

      // cannot indent null tasks
      Task task = get( row );
      if ( task.isNull() )
        continue;

      // cannot indent tasks already indented compared to task above
      int above = row - 1;
      while ( get( above ).isNull() )
        above--;
      if ( task.getIndent() > get( above ).getIndent() )
        continue;

      can.add( row );
    }

    return can;
  }

  /***************************************** canOutdent ******************************************/
  public Set<Integer> canOutdent( Set<Integer> rows )
  {
    // return the subset of rows that can be outdented
    Set<Integer> can = new HashSet<Integer>();
    for ( int row : rows )
    {
      // cannot outdent hidden task 0 or task 1
      if ( row <= 1 )
        continue;

      // cannot outdent null tasks
      Task task = get( row );
      if ( task.isNull() )
        continue;

      // cannot outdent tasks with no indent
      if ( task.getIndent() == 0 )
        continue;

      can.add( row );
    }

    return can;
  }

  /************************************ updateSummaryMarkers *************************************/
  public void updateSummaryMarkers()
  {
    // for each task ensure summaryEnd and SummaryStart set correctly
    for ( int row = 0; row < size(); row++ )
    {
      Task task = get( row );
      if ( task.isNull() )
        continue;

      int indent = task.getIndent();

      // check if summary, and if summary set summary end
      Task other;
      int end = -1;
      int check = row;
      while ( ++check < size() )
      {
        other = get( check );
        if ( other.isNull() )
          continue;

        if ( other.getIndent() <= indent )
          break;
        else
          end = check;
      }
      task.setSummaryEnd( end );

      // look for summary of this task
      for ( int start = row - 1; start >= 0; start-- )
      {
        other = get( start );
        if ( other.isNull() )
          continue;

        if ( other.getIndent() < indent )
        {
          task.setSummaryStart( start );
          break;
        }
      }
    }

    // ensure special task 0 is marked as summary
    if ( get( 0 ).getSummaryEnd() < 0 )
      get( 0 ).setSummaryEnd( size() - 1 );
  }

  /************************************** cleanPredecessors **************************************/
  public PredecessorsList cleanPredecessors()
  {
    // clean all predecessors, returning list of those modified before change
    PredecessorsList list = new PredecessorsList();

    for ( int id = 1; id < size(); id++ )
    {
      Task task = get( id );
      if ( task.isNull() )
        continue;

      Predecessors pred = task.getPredecessors();
      String before = pred.toString();
      pred.clean( id );
      if ( !before.equals( pred.toString() ) )
        list.put( id, before );
    }

    return list;
  }

  /************************************* restorePredecessors *************************************/
  public void restorePredecessors( PredecessorsList list )
  {
    // restore cleaned predecessors
    for ( HashMap.Entry<Integer, String> entry : list.entrySet() )
      get( entry.getKey() ).setValue( Task.SECTION_PRED, new Predecessors( entry.getValue() ) );
  }

  /************************************** getTaskResources ***************************************/
  public HashMap<Task, TaskResources> getTaskResources( String tag )
  {
    // return map of tasks and TaskResources that use the tag
    HashMap<Task, TaskResources> map = new HashMap<Task, TaskResources>();
    for ( Task task : this )
    {
      TaskResources tr = (TaskResources) task.getValue( Task.SECTION_RES );
      if ( tr != null && tr.containsTag( tag ) )
        map.put( task, tr );
    }

    return map;
  }

}