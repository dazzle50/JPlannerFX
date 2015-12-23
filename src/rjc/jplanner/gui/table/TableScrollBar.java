/**************************************************************************
 *  Copyright (C) 2015 by Richard Crook                                   *
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

package rjc.jplanner.gui.table;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollBar;
import rjc.jplanner.JPlanner;

/*************************************************************************************************/
/*************************** Scroll bar for tables that self-manage ******************************/
/*************************************************************************************************/

public class TableScrollBar extends ScrollBar
{
  public static final double SIZE = 18.0;
  public static Canvas       corner;

  private Table              m_table;
  private TableScrollBar     m_other;

  /**************************************** constructor ******************************************/
  public TableScrollBar( Table table, Orientation orientation )
  {
    // create table scroll bar
    super();
    m_table = table;
    setOrientation( orientation );
    setMinWidth( SIZE );
    setMinHeight( SIZE );

    // create corner that sits between the two scroll bars
    if ( corner == null )
    {
      corner = new Canvas( SIZE, SIZE );
      GraphicsContext gc = corner.getGraphicsContext2D();
      gc.setFill( Table.COLOR_HEADER_FILL );
      gc.fillRect( 0, 0, getWidth(), getHeight() );
    }

    // add listener to ensure scroll bar appropriate for table body size
    ReadOnlyDoubleProperty property = m_table.getBody().heightProperty();
    if ( orientation == Orientation.HORIZONTAL )
      property = m_table.getBody().widthProperty();
    property.addListener( new ChangeListener<Number>()
    {
      @Override
      public void changed( ObservableValue<? extends Number> observable, Number oldValue, Number newValue )
      {
        check();
        m_other.check();
      }
    } );

    // ******************************************** TODO ................
    valueProperty().addListener( new ChangeListener<Number>()
    {
      @Override
      public void changed( ObservableValue<? extends Number> observable, Number oldValue, Number newValue )
      {
        // TODO .............
        JPlanner.trace( TableScrollBar.this.toString() );
      }
    } );

  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    return "TableScrollBar " + getOrientation().toString().charAt( 0 ) + " value=" + getValue() + " thumb="
        + getVisibleAmount() + " min=" + getMin() + " max=" + getMax();
  }

  /****************************************** setOther *******************************************/
  public void setOther( TableScrollBar other )
  {
    m_other = other;
  }

  /***************************************** setLength *******************************************/
  private void setLength( int size )
  {
    // set scroll-bar length
    if ( getOrientation() == Orientation.HORIZONTAL )
    {
      // horizontal scroll bar
      m_table.getChildren().remove( this );
      m_table.add( this, 0, 2, size, 1 );
    }
    else
    {
      // vertical scroll bar
      m_table.getChildren().remove( this );
      m_table.add( this, 2, 0, 1, size );
    }
  }

  /***************************************** setLengths ******************************************/
  private void setLengths()
  {
    // ensure scroll bar lengths are correct for those visible
    if ( isVisible() && m_other.isVisible() )
    {
      // both visible so both need length of 2
      setLength( 2 );
      m_other.setLength( 2 );

      // show scroll bar corner
      corner.setVisible( true );
    }
    else
    {
      // hide scroll bar corner
      corner.setVisible( false );

      // if visible set length to 3
      if ( isVisible() )
        setLength( 3 );
      if ( m_other.isVisible() )
        m_other.setLength( 3 );
    }
  }

  /******************************************** check ********************************************/
  private void check()
  {
    // check ensure scroll bar appropriate for table body size
    double size = 0.0;
    int index, max;
    if ( !m_other.isVisible() )
      size += SIZE;
    if ( getOrientation() == Orientation.VERTICAL )
    {
      size += m_table.getBody().getHeight();
      index = m_table.getRowExactAtY( size );
      max = m_table.getRowStartY( Integer.MAX_VALUE );
    }
    else
    {
      size += m_table.getBody().getWidth();
      index = m_table.getColumnExactAtX( size );
      max = m_table.getColumnStartX( Integer.MAX_VALUE );
    }

    // scroll bar needed if normal column/row index at edge
    boolean needed = index != Integer.MAX_VALUE;
    if ( isVisible() != needed )
    {
      setVisible( needed );
      setLengths();
    }

    // if visible, check thumb size
    if ( isVisible() )
    {
      setMax( max );
      setVisibleAmount( size );
    }

  }

}