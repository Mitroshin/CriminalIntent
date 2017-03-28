package com.developgmail.mitroshin.criminalintent;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class PhotoFragment extends DialogFragment{

    public static final String ARG_PHOTO = "photo";
    private ImageView photo;

    public static PhotoFragment newInstance(String path) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_PHOTO, path);

        PhotoFragment fragment = new PhotoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_photo, null);
        String pathToPhoto = (String) getArguments().getSerializable(ARG_PHOTO);
        photo = (ImageView) v.findViewById(R.id.dialog_image_view);
        if (pathToPhoto != null) {
            Bitmap bitmap = PicturesUtil.getScaledBitmap(pathToPhoto, getActivity());
            photo.setImageBitmap(bitmap);
        } else {
            Toast.makeText(getActivity(), "You need to create photo", Toast.LENGTH_SHORT).show();
        }
        return new AlertDialog.Builder(getActivity()).setView(v).create();
    }
}
