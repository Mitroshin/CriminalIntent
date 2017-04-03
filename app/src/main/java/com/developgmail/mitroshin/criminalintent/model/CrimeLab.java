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
    private List<Crime> crimeList;

    public static CrimeLab get(Context context) {
        if (sCrimeLab == null) {
            sCrimeLab = new CrimeLab(context);
        }
        return sCrimeLab;
    }

    private CrimeLab (Context context) {
        mContext = context.getApplicationContext();
        mDatabase = new CrimeBaseHelper(mContext).getWritableDatabase();
    }

    public List<Crime> getCrimes() {
        crimeList = new ArrayList<>();
        tryToFillCrimeList();
        return crimeList;
    }

    private void tryToFillCrimeList() {
        CrimeCursorWrapper cursorWrapper = queryCrimes(null, null);
        try {
            fillCrimeList(cursorWrapper);
        } finally {
            cursorWrapper.close();
        }
    }

    private void fillCrimeList(CrimeCursorWrapper cursorWrapper) {
        cursorWrapper.moveToFirst();
        while(!cursorWrapper.isAfterLast()) {
            crimeList.add(cursorWrapper.getCrime());
            cursorWrapper.moveToNext();
        }
    }

    public Crime getCrime(UUID id) {
        CrimeCursorWrapper cursorWrapper = queryCrimes(
                CrimeTable.Cols.UUID + " = ?",
                new String[] { id.toString() }
        );

        try {
            if (cursorWrapper.getCount() == 0) {
                return null;
            }

            cursorWrapper.moveToFirst();
            return cursorWrapper.getCrime();
        } finally {
            cursorWrapper.close();
        }
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
        ContentValues values = new ContentValues();
        values.put(CrimeTable.Cols.UUID, crime.getId().toString());
        values.put(CrimeTable.Cols.TITLE, crime.getTitle());
        values.put(CrimeTable.Cols.DATE, crime.getDate().getTime());
        values.put(CrimeTable.Cols.SOLVED, crime.isSolved() ? 1 : 0);
        values.put(CrimeTable.Cols.SUSPECT, crime.getSuspect());
        values.put(CrimeTable.Cols.SUSPECT_ID, crime.getSuspectId());

        return values;
    }

    public void updateCrime(Crime crime) {
        String uuidString = crime.getId().toString();
        ContentValues values = getContentValues(crime);
        mDatabase.update(CrimeTable.NAME, values, CrimeTable.Cols.UUID + " = ?",
                new String[] { uuidString });
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

    public File getPhotoFile(Crime crime) {
        File externalFileDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (externalFileDir == null) {
            return null;
        } else {
            return new File(externalFileDir, crime.getPhotoFileName());
        }
    }
}
