package com.developgmail.mitroshin.criminalintent.fragment;

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

import com.developgmail.mitroshin.criminalintent.util.PicturesUtil;
import com.developgmail.mitroshin.criminalintent.R;
import com.developgmail.mitroshin.criminalintent.fragment.dialog.DatePickerFragment;
import com.developgmail.mitroshin.criminalintent.fragment.dialog.PhotoFragment;
import com.developgmail.mitroshin.criminalintent.fragment.dialog.TimePickerFragment;
import com.developgmail.mitroshin.criminalintent.model.Crime;
import com.developgmail.mitroshin.criminalintent.model.CrimeLab;

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
    private static final String DIALOG_PHOTO = "DialogPhoto";
    private static final String TIME_TEMPLATE = "kk : mm";
    private static final String DATE_TEMPLATE = "EEEE, MMMM d, yyyyy";

    private Crime mCurrentCrime;
    private File mPhotoFile;
    private PackageManager mPackageManager;
    private View mViewLayout;
    private FragmentManager mFragmentManager;
    private Callbacks mCallbacks;

    private EditText mTitleField;
    private Button mDateButton;
    private Button mTimeButton;
    private CheckBox mSolvedCheckBox;
    private Button mReportButton;
    private Button mSuspectButton;
    private ImageButton mCallButton;
    private ImageButton mPhotoButton;
    private ImageView mPhotoView;

    public interface Callbacks {
        void OnCrimeUpdated(Crime crime);
    }

    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        mCurrentCrime = CrimeLab.get(getActivity()).getCrimeById(crimeId);
        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCurrentCrime);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mViewLayout = inflater.inflate(R.layout.fragment_crime, container, false);
        mPackageManager = getActivity().getPackageManager();
        mFragmentManager = getFragmentManager();
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
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCurrentCrime.setTitle(s.toString());
                updateCrime();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void afterTextChanged(Editable s) { }
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
        DatePickerFragment dialogDate = DatePickerFragment.newInstance(mCurrentCrime.getDate());
        dialogDate.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
        dialogDate.show(mFragmentManager, DIALOG_DATE);
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
        TimePickerFragment dialogTime = TimePickerFragment.newInstance(mCurrentCrime.getDate());
        dialogTime.setTargetFragment(CrimeFragment.this, REQUEST_TIME);
        dialogTime.show(mFragmentManager, DIALOG_TIME);
    }

    private void initViewCrimeSolved() {
        mSolvedCheckBox = (CheckBox) mViewLayout.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCurrentCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCurrentCrime.setSolved(isChecked);
                updateCrime();
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
        return getString(R.string.crime_report,
                mCurrentCrime.getTitle(),
                getCurrentDateAsString(),
                getCurrentSolvedAsString(),
                getCurrentSuspectAsString());
    }

    private String getCurrentDateAsString() {
        return DateFormat.format(DATE_TEMPLATE, mCurrentCrime.getDate()).toString();
    }

    private String getCurrentSolvedAsString() {
        if (mCurrentCrime.isSolved()) {
            return getString(R.string.crime_report_solved);
        } else {
            return getString(R.string.crime_report_unsolved);
        }
    }

    private String getCurrentSuspectAsString() {
        String suspectString = mCurrentCrime.getSuspect();
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
        requestPermissions(
                new String[] { Manifest.permission.READ_CONTACTS },
                REQUEST_PERMISSION_CONTACTS);
    }

    private void initViewCalToSuspect() {
        mCallButton = (ImageButton) mViewLayout.findViewById(R.id.call_to_suspect);
        mCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSuspectSelected() && isPermissionToCall()) {
                    Cursor cursorNumber = getCursorNumber();
                    if (cursorNumber != null && cursorNumber.getCount() > 0) {
                        tryCallToSuspect(cursorNumber);
                    }
                } else {
                    requestPermissionToCall();
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
        requestPermissions(
                new String[] { Manifest.permission.CALL_PHONE },
                REQUEST_PERMISSION_CALL);
    }

    private void initViewCrimeCamera() {
        // TODO Метод надо переделать. Мне не нравится.
        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mPhotoButton = (ImageButton) mViewLayout.findViewById(R.id.crime_camera);
        if (!canTakePhoto(captureImage)) {
            mPhotoButton.setEnabled(false);
            return;
        }
        Uri uriToResultImage = Uri.fromFile(mPhotoFile);
        captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uriToResultImage);

        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(captureImage, REQUEST_PHOTO);
            }
        });
    }

    private boolean canTakePhoto(Intent captureImage) {
        return mPhotoFile != null && captureImage.resolveActivity(mPackageManager) != null;
    }

    private void initViewCrimePhoto() {
        mPhotoView = (ImageView) mViewLayout.findViewById(R.id.crime_photo);
        updatePhotoOnView();
        mPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchPhotoDialog();
            }
        });
    }

    private void launchPhotoDialog() {
        PhotoFragment dialogPhoto = PhotoFragment.newInstance(mPhotoFile.getPath());
        dialogPhoto.show(mFragmentManager, DIALOG_PHOTO);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_DATE:
                setDateFromExtraData(data);
                updateCrime();
                updateDateOnView();
                break;
            case REQUEST_TIME:
                setDateFromExtraData(data);
                updateCrime();
                updateTimeOnView();
                break;
            case REQUEST_PHOTO:
                updateCrime();
                updatePhotoOnView();
                break;
            case REQUEST_CONTACT:
                if (data == null) {
                    return;
                }
                Cursor cursorContacts = getCursorName(data);
                if (cursorContacts != null && cursorContacts.getCount() != 0) {
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

        return getActivity().getContentResolver()
                .query(contactUri, queryFields, null, null, null);
    }

    private void trySetSelectedSuspect(Cursor cursorContacts) {
        // TODO Уточнить - правильно ли используется в данном случае выделение блока try в отдельный метод
        try {
            setSelectedSuspect(cursorContacts);
        } finally {
            cursorContacts.close();
        }
    }

    private void setSelectedSuspect(Cursor cursorContacts) {
        cursorContacts.moveToFirst();
        String suspect = cursorContacts.getString(0);
        long contactId = cursorContacts.getLong(1);
        updateSuspectData(suspect, contactId);
    }

    private void updateSuspectData(String suspect, long contactId) {
        mCurrentCrime.setSuspect(suspect);
        mCurrentCrime.setSuspectId(contactId);
        updateCrime();
        mSuspectButton.setText(suspect);
    }

    private void updateCrime() {
        CrimeLab.get(getActivity()).updateCrime(mCurrentCrime);
        mCallbacks.OnCrimeUpdated(mCurrentCrime);
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
                    Toast.makeText(getActivity(), "Permission to contact denied", Toast.LENGTH_SHORT).show();
                }
                return;
            case REQUEST_PERMISSION_CALL:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Cursor cursorNumber = getCursorNumber();
                    if (cursorNumber != null && cursorNumber.getCount() > 0) {
                        tryCallToSuspect(cursorNumber);
                    }
                } else {
                    Toast.makeText(getActivity(), "Permission to call denied", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private Cursor getCursorNumber() {
        Uri contentUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;

        String selectClause = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
        String[] fields = { ContactsContract.CommonDataKinds.Phone.NUMBER };
        String[] selectParams = { Long.toString(mCurrentCrime.getSuspectId()) };

        return getActivity().getContentResolver()
                .query(contentUri, fields, selectClause, selectParams, null);
    }

    private void tryCallToSuspect(Cursor cursorNumber) {
        try {
            Uri phoneNumber = getPhoneNumberFromCursor(cursorNumber);
            callToSuspect(phoneNumber);
        } finally {
            cursorNumber.close();
        }
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

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }
}
