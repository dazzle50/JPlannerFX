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

package rjc.jplanner.gui.table;

import javafx.beans.value.ChangeListener;
import rjc.jplanner.gui.DateTimeEditor;
import rjc.jplanner.model.Date;
import rjc.jplanner.model.DateTime;
import rjc.jplanner.model.Time;

/*************************************************************************************************/
/***************************** Table cell editor for DateTime fields *****************************/
/*************************************************************************************************/

public class EditorDateTime extends AbstractCellEditor
{
  private DateTimeEditor m_editor; // date-time editor

  /**************************************** constructor ******************************************/
  public EditorDateTime( int columnIndex, int row )
  {
    // create date-time table cell editor
    super( columnIndex, row );
    m_editor = new DateTimeEditor();
    setControl( m_editor );
  }

  /*********************************** addMillisecondsListener ***********************************/
  public void addMillisecondsListener( ChangeListener<? super Number> listener )
  {
    // add listener to milliseconds long property
    m_editor.addListener( listener );
  }

  /******************************************* getValue ******************************************/
  @Override
  public Object getValue()
  {
    // return value date-time
    return m_editor.getDateTime();
  }

  /******************************************* setValue ******************************************/
  @Override
  public void setValue( Object value )
  {
    // set value depending on type
    if ( value == null )
      m_editor.setDateTime( new DateTime( Date.now(), Time.MAX_VALUE ) );
    else if ( value instanceof DateTime )
      m_editor.setDateTime( (DateTime) value );
    else if ( value instanceof String )
    {
      // seed editor with suitable date before setting with input string
      setValue( (DateTime) getDataSourceValue() );
      m_editor.setCaretPos( ( (String) value ).length() );
      m_editor.setText( (String) value );
    }
    else
      throw new IllegalArgumentException( "Don't know how to handle " + value.getClass() + " " + value );
  }

}
