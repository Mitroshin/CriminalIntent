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
                UUID crimeId = mCurrentCrime.getId();
                CrimeLab.get(getActivity()).deleteCrime(crimeId);
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        setViewCrimeTitle();
        setViewCrimeDate();
        setViewCrimeTime();
        setViewCrimeSolved();
        setViewCrimeReport();
        setViewCrimeSuspect();
        setViewCalToSuspect();
        setViewCrimeCamera();
        setViewCrimePhoto();
    }

    private void setViewCrimeTitle() {
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

    private void setViewCrimeDate() {
        mDateButton = (Button) mViewLayout.findViewById(R.id.crime_date);
        updateDateOnView();
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                DatePickerFragment dialog = DatePickerFragment.newInstance(mCurrentCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                dialog.show(manager, DIALOG_DATE);
            }
        });
    }

    private void setViewCrimeTime() {
        mTimeButton = (Button) mViewLayout.findViewById(R.id.crime_time);
        updateTimeOnView();
        mTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                TimePickerFragment dialog = TimePickerFragment.newInstance(mCurrentCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_TIME);
                dialog.show(manager, DIALOG_TIME);
            }
        });
    }

    private void setViewCrimeSolved() {
        mSolvedCheckBox = (CheckBox) mViewLayout.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCurrentCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCurrentCrime.setSolved(isChecked);
            }
        });
    }

    private void setViewCrimeReport() {
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
        String solvedString = null;
        if (mCurrentCrime.isSolved()) {
            solvedString = getString(R.string.crime_report_solved);
        } else {
            solvedString = getString(R.string.crime_report_unsolved);
        }

        String dateString = DateFormat.format(DATE_TEMPLATE, mCurrentCrime.getDate()).toString();

        String suspect = Long.toString(mCurrentCrime.getSuspectId());
        if (suspect == null) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }

        String report = getString(R.string.crime_report, mCurrentCrime.getTitle(),
                dateString, solvedString, suspect);

        return report;
    }

    private void setViewCrimeSuspect() {
        final Intent pickContact = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton = (Button) mViewLayout.findViewById(R.id.crime_suspect);
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                        android.Manifest.permission.READ_CONTACTS);

                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[] {
                            Manifest.permission.READ_CONTACTS
                    }, REQUEST_PERMISSION_CONTACTS);
                } else {
                    startActivityForResult(pickContact, REQUEST_CONTACT);
                }
            }
        });

        if (mCurrentCrime.getSuspectId() != 0) {
            mSuspectButton.setText(Long.toString(mCurrentCrime.getSuspectId()));
        }

        if (mPackageManager.resolveActivity(pickContact, PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        }
    }

    private void setViewCalToSuspect() {
        mCallButton = (ImageButton) mViewLayout.findViewById(R.id.call_to_suspect);
        mCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mCurrentCrime.getSuspect() == null) {
                    Toast.makeText(CrimeFragment.this.getActivity()
                            , "You need to choose suspect"
                            , Toast.LENGTH_SHORT).show();
                    return;
                }

                int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.CALL_PHONE);

                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[] {
                            Manifest.permission.CALL_PHONE
                    }, REQUEST_PERMISSION_CALL);
                } else {
                    getNumberAndCall();
                }


            }
        });
    }

    private void setViewCrimeCamera() {
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

    private void setViewCrimePhoto() {
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
                    cursorContacts = getCursorFromContacts(data);
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

    private Cursor getCursorFromContacts(Intent data) {
        Uri contactUri = data.getData();

        String[] queryFields = new String[] {
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts._ID
        };

        Cursor cursorFromContacts = getActivity().getContentResolver()
                .query(contactUri, queryFields, null, null, null);

        return cursorFromContacts;
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
                    getNumberAndCall();
                } else {
                    Toast.makeText(getActivity(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void getNumberAndCall() {
        Uri contentUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String selectClause = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
        String[] fields = {
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        String[] selectParams = {
                Long.toString(mCurrentCrime.getSuspectId())
        };
        Cursor cursor = getActivity().getContentResolver()
                .query(contentUri, fields, selectClause, selectParams, null);

        if (cursor != null && cursor.getCount() > 0) {
            try {
                cursor.moveToFirst();
                String number = cursor.getString(0);
                Uri phoneNumber = Uri.parse("tel:" + number);

                Intent intent = new Intent(Intent.ACTION_DIAL, phoneNumber);
                startActivity(intent);
            } finally {
                cursor.close();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        CrimeLab.get(getActivity()).updateCrime(mCurrentCrime);
    }
}
