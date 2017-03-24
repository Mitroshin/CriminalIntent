package com.developgmail.mitroshin.criminalintent;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ShareCompat.IntentBuilder;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.Date;
import java.util.UUID;

public class CrimeFragment extends Fragment{

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_TIME = 1;
    private static final int REQUEST_CONTACT = 2;
    private static final int REQUEST_PHOTO = 3;
    private static final int REQUEST_PERMISSION_CALL = 4;
    private static final int REQUEST_PERMISSION_CONTACTS = 5;

    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";
    private static final String DIALOG_TIME = "DialogTime";
    private static final String TIME_TEMPLATE = "kk : mm";
    private static final String DATE_TEMPLATE = "EEEE, MMMM d, yyyyy";

    private Crime mCurrentCrime;
    private EditText mTitleField;
    private Button mDateButton;
    private Button mTimeButton;
    private CheckBox mSolvedCheckBox;
    private Button mReportButton;
    private Button mSuspectButton;
    private ImageButton mCallButton;
    private ImageButton mPhotoButton;
    private ImageView mPhotoView;
    private File mPhotoFile;
    private PackageManager mPackageManager;
    private View mViewLayout;

    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPackageManager = getActivity().getPackageManager();
        setHasOptionsMenu(true);
        setCurrentCrime();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_delete_crime:
                deleteCurrentCrime();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void deleteCurrentCrime() {
        UUID crimeId = mCurrentCrime.getId();
        CrimeLab.get(getActivity()).deleteCrime(crimeId);
        getActivity().finish();
    }

    private void setCurrentCrime() {
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCurrentCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCurrentCrime);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mViewLayout = inflater.inflate(R.layout.fragment_crime, container, false);
        initializeLayout();
        return mViewLayout;
    }

    private void initializeLayout() {
        initViewCrimeTitle();
        initViewCrimeDate();
        initViewCrimeTime();
        initViewCrimeSolved();
        initViewCrimeReport();
        initViewCrimeSuspect();
        initViewCalToSuspect();
        initViewCrimeCamera();
        initViewCrimePhoto();
    }

    private void initViewCrimeTitle() {
        mTitleField = (EditText) mViewLayout.findViewById(R.id.crime_title);
        mTitleField.setText(mCurrentCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCurrentCrime.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void initViewCrimeDate() {
        mDateButton = (Button) mViewLayout.findViewById(R.id.crime_date);
        updateDateOnView();
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchDatePickerDialog();
            }
        });
    }

    private void launchDatePickerDialog() {
        FragmentManager manager = getFragmentManager();
        DatePickerFragment dialogDate = DatePickerFragment.newInstance(mCurrentCrime.getDate());
        dialogDate.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
        dialogDate.show(manager, DIALOG_DATE);
    }

    private void initViewCrimeTime() {
        mTimeButton = (Button) mViewLayout.findViewById(R.id.crime_time);
        updateTimeOnView();
        mTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchTimePickerDialog();
            }
        });
    }

    private void launchTimePickerDialog() {
        FragmentManager manager = getFragmentManager();
        TimePickerFragment dialogTime = TimePickerFragment.newInstance(mCurrentCrime.getDate());
        dialogTime.setTargetFragment(CrimeFragment.this, REQUEST_TIME);
        dialogTime.show(manager, DIALOG_TIME);
    }

    private void initViewCrimeSolved() {
        mSolvedCheckBox = (CheckBox) mViewLayout.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCurrentCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCurrentCrime.setSolved(isChecked);
            }
        });
    }

    private void initViewCrimeReport() {
        mReportButton = (Button) mViewLayout.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(getCrimeReport())
                        .setSubject(getString(R.string.crime_report_subject))
                        .setChooserTitle(R.string.send_report)
                        .startChooser();
            }
        });
    }

    private String getCrimeReport() {
        String report = getString(R.string.crime_report,
                mCurrentCrime.getTitle(),
                getCurrentDateAsString(),
                getCurrentSolvedAsString(),
                getCurrentSuspectAsString());
        return report;
    }

    private String getCurrentSolvedAsString() {
        if (mCurrentCrime.isSolved()) {
            return getString(R.string.crime_report_solved);
        } else {
            return getString(R.string.crime_report_unsolved);
        }
    }

    private String getCurrentDateAsString() {
        return DateFormat.format(DATE_TEMPLATE, mCurrentCrime.getDate()).toString();
    }

    private String getCurrentSuspectAsString() {
        String suspectString;
        suspectString = Long.toString(mCurrentCrime.getSuspectId());
        if (suspectString == null) {
            return getString(R.string.crime_report_no_suspect);
        } else {
            return getString(R.string.crime_report_suspect, suspectString);
        }
    }

    private void initViewCrimeSuspect() {
        final Intent pickContact = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton = (Button) mViewLayout.findViewById(R.id.crime_suspect);
        checkContactAppIsExist(pickContact);
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPermissionToReadContact()) {
                    startActivityForResult(pickContact, REQUEST_CONTACT);
                } else {
                    requestPermissionToReadContact();
                }
            }

        });
    }

    private void checkContactAppIsExist(Intent pickContact) {
        if (mPackageManager.resolveActivity(pickContact, PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        }
    }

    private boolean isPermissionToReadContact() {
        int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.READ_CONTACTS);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionToReadContact() {
        requestPermissions(new String[] {
                Manifest.permission.READ_CONTACTS
            },
            REQUEST_PERMISSION_CONTACTS);
    }

    private void initViewCalToSuspect() {
        mCallButton = (ImageButton) mViewLayout.findViewById(R.id.call_to_suspect);
        mCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSuspectSelected()) {
                    if (isPermissionToCall()) {
                        tryCallToSuspect();
                    } else {
                        requestPermissionToCall();
                    }
                } else {
                    Toast.makeText(CrimeFragment.this.getActivity()
                            , "You need to choose suspect"
                            , Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean isSuspectSelected() {
        return mCurrentCrime.getSuspect() != null;
    }

    private boolean isPermissionToCall() {
        int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.CALL_PHONE);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionToCall() {
        requestPermissions(new String[] {
                Manifest.permission.CALL_PHONE
        },
        REQUEST_PERMISSION_CALL);
    }

    private void initViewCrimeCamera() {
        mPhotoButton = (ImageButton) mViewLayout.findViewById(R.id.crime_camera);
        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        boolean canTakePhoto =
                mPhotoFile != null && captureImage.resolveActivity(mPackageManager) != null;
        mPhotoButton.setEnabled(canTakePhoto);
        if (canTakePhoto) {
            Uri uri = Uri.fromFile(mPhotoFile);
            captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }
        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(captureImage, REQUEST_PHOTO);
            }
        });
    }

    private void initViewCrimePhoto() {
        mPhotoView = (ImageView) mViewLayout.findViewById(R.id.crime_photo);
        updatePhotoOnView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_DATE:
                setDateFromExtraData(data);
                updateDateOnView();
                break;
            case REQUEST_TIME:
                setDateFromExtraData(data);
                updateTimeOnView();
                break;
            case REQUEST_PHOTO:
                updatePhotoOnView();
                break;
            case REQUEST_CONTACT:
                Cursor cursorContacts = null;
                if (data != null) {
                    cursorContacts = getCursorName(data);
                }
                if (cursorContacts != null) {
                    trySetSelectedSuspect(cursorContacts);
                }
                break;
        }
    }

    private void setDateFromExtraData(Intent data) {
        Date date = (Date) data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);
        mCurrentCrime.setDate(date);
    }

    private void updateDateOnView() {
        Date date = mCurrentCrime.getDate();
        mDateButton.setText(DateFormat.format(DATE_TEMPLATE, date));
    }

    private void updateTimeOnView() {
        Date time = mCurrentCrime.getDate();
        mTimeButton.setText(DateFormat.format(TIME_TEMPLATE, time));
    }

    private void updatePhotoOnView() {
        if (mPhotoFile == null || !mPhotoFile.exists()) {
            mPhotoView.setImageDrawable(null);
        } else {
            // TODO Скорее всего надо бы выделить в отдельный метод
            Bitmap bitmap = PicturesUtil.getScaledBitmap(mPhotoFile.getPath(), getActivity());
            mPhotoView.setImageBitmap(bitmap);
        }
    }

    private Cursor getCursorName(Intent data) {
        Uri contactUri = data.getData();

        String[] queryFields = new String[] {
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts._ID
        };

        Cursor cursorName = getActivity().getContentResolver()
                .query(contactUri, queryFields, null, null, null);

        return cursorName;
    }

    private void trySetSelectedSuspect(Cursor cursorContacts) {
        try {
            setSelectedSuspect(cursorContacts);
        } finally {
            cursorContacts.close();
        }
    }

    private void setSelectedSuspect(Cursor cursorContacts) {
        if (cursorContacts.getCount() == 0) {
            return;
        }
        cursorContacts.moveToFirst();
        String suspect = cursorContacts.getString(0);
        long contactId = cursorContacts.getLong(1);
        updateSuspectOnView(suspect, contactId);
    }

    private void updateSuspectOnView(String suspect, long contactId) {
        mCurrentCrime.setSuspect(suspect);
        mCurrentCrime.setSuspectId(contactId);
        mSuspectButton.setText(suspect);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_CONTACTS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                    startActivityForResult(pickContact, REQUEST_CONTACT);
                } else {
                    Toast.makeText(getActivity(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            case REQUEST_PERMISSION_CALL:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    tryCallToSuspect();
                } else {
                    Toast.makeText(getActivity(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void tryCallToSuspect() {
        Cursor cursorNumber = getCursorNumber();
        if (cursorNumber != null && cursorNumber.getCount() > 0) {
            try {
                Uri phoneNumber = getPhoneNumberFromCursor(cursorNumber);
                callToSuspect(phoneNumber);
            } finally {
                cursorNumber.close();
            }
        }
    }

    private Cursor getCursorNumber() {
        Uri contentUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;

        String selectClause = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
        String[] fields = {
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        String[] selectParams = {
                Long.toString(mCurrentCrime.getSuspectId())
        };

        Cursor cursorNumber = getActivity().getContentResolver()
                .query(contentUri, fields, selectClause, selectParams, null);

        return cursorNumber;
    }

    private Uri getPhoneNumberFromCursor(Cursor cursor) {
        cursor.moveToFirst();
        String number = cursor.getString(0);
        return Uri.parse("tel:" + number);
    }

    private void callToSuspect(Uri phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL, phoneNumber);
        startActivity(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        CrimeLab.get(getActivity()).updateCrime(mCurrentCrime);
    }
}
