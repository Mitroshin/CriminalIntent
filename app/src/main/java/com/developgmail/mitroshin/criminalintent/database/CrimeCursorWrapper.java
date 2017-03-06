package com.developgmail.mitroshin.criminalintent.database;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.developgmail.mitroshin.criminalintent.Crime;
import com.developgmail.mitroshin.criminalintent.database.CrimeDBSchema.CrimeTable;

public class CrimeCursorWrapper extends CursorWrapper {
    public CrimeCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Crime getCrime() {
        String uuidString = getString(getColumnIndex(CrimeTable.Cols.UUID));
        String title = getString(getColumnIndex(CrimeTable.Cols.TITLE));
        long date = getLong(getColumnIndex(CrimeTable.Cols.DATE));
        int isSolved = getInt(getColumnIndex(CrimeTable.Cols.SOLVED));

        return null;
    }
}
