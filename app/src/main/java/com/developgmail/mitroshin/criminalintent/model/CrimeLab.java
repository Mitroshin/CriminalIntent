package com.developgmail.mitroshin.criminalintent.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import com.developgmail.mitroshin.criminalintent.database.CrimeBaseHelper;
import com.developgmail.mitroshin.criminalintent.database.CrimeCursorWrapper;
import com.developgmail.mitroshin.criminalintent.database.CrimeDBSchema.CrimeTable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CrimeLab {
    private static CrimeLab sCrimeLab;
    private Context mContext;
    private SQLiteDatabase mDatabase;
    private List<Crime> crimeGroup;

    public static CrimeLab get(Context context) {
        if (sCrimeLab == null) {
            sCrimeLab = new CrimeLab(context);
        }
        return sCrimeLab;
    }

    private CrimeLab(Context context) {
        mContext = context.getApplicationContext();
        mDatabase = new CrimeBaseHelper(mContext).getWritableDatabase();
    }

    public List<Crime> getCrimeGroup() {
        crimeGroup = new ArrayList<>();
        fillCrimeGroup();
        return crimeGroup;
    }

    private void fillCrimeGroup() {
        CrimeCursorWrapper cursorOfCrimeGroup = queryCrimes(null, null);
        try {
            tryToFillCrimeGroup(cursorOfCrimeGroup);
        } finally {
            cursorOfCrimeGroup.close();
        }
    }

    private void tryToFillCrimeGroup(CrimeCursorWrapper cursorOfCrimeGroup) {
        cursorOfCrimeGroup.moveToFirst();
        while(!cursorOfCrimeGroup.isAfterLast()) {
            crimeGroup.add(cursorOfCrimeGroup.getCrime());
            cursorOfCrimeGroup.moveToNext();
        }
    }

    public Crime getCrimeById(UUID id) {
        CrimeCursorWrapper cursorOfCrime = getCursorOfCrimeById(id);
        Crime crime;
        try {
            crime = getFirstCrimeFromCursor(cursorOfCrime);
            return crime;
        } finally {
            cursorOfCrime.close();
        }
    }

    private CrimeCursorWrapper getCursorOfCrimeById(UUID id) {
        return queryCrimes(
                CrimeTable.Cols.UUID + " = ?",
                new String[] { id.toString() }
        );
    }

    private Crime getFirstCrimeFromCursor(CrimeCursorWrapper cursorOfCrime) {
        if (cursorOfCrime.getCount() == 0) {
            return null;
        }
        cursorOfCrime.moveToFirst();
        return cursorOfCrime.getCrime();
    }

    private CrimeCursorWrapper queryCrimes(String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(
                CrimeTable.NAME,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                null
        );
        return new CrimeCursorWrapper(cursor);
    }

    public void addCrime(Crime crime) {
        ContentValues values = getContentValues(crime);
        mDatabase.insert(CrimeTable.NAME, null, values);
    }

    public void deleteCrime(UUID crimeId) {
        mDatabase.delete(CrimeTable.NAME, CrimeTable.Cols.UUID + " = ?",
                new String[] { crimeId.toString() });
    }

    private static ContentValues getContentValues(Crime crime) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CrimeTable.Cols.UUID, crime.getId().toString());
        contentValues.put(CrimeTable.Cols.TITLE, crime.getTitle());
        contentValues.put(CrimeTable.Cols.DATE, crime.getDate().getTime());
        contentValues.put(CrimeTable.Cols.SOLVED, crime.isSolved() ? 1 : 0);
        contentValues.put(CrimeTable.Cols.SUSPECT, crime.getSuspect());
        contentValues.put(CrimeTable.Cols.SUSPECT_ID, crime.getSuspectId());

        return contentValues;
    }

    public void updateCrime(Crime crime) {
        String crimeId = crime.getId().toString();
        ContentValues values = getContentValues(crime);
        mDatabase.update(CrimeTable.NAME, values, CrimeTable.Cols.UUID + " = ?",
                new String[] { crimeId });
    }

    public File getPhotoFile(Crime crime) {
        File externalFileDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (externalFileDir == null) {
            return null;
        } else {
            return new File(externalFileDir, crime.getPhotoFileName());
        }
    }
}
