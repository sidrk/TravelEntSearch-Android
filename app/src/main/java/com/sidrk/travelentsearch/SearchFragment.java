package com.sidrk.travelentsearch;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.seatgeek.placesautocomplete.PlacesAutocompleteTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class SearchFragment extends Fragment {

    private static final String TAG = "SearchFragment";

    private static final List<String> CATEGORIES = Arrays.asList("Default", "Airport", "Amusement Park", "Aquarium", "Art Gallery", "Bakery", "Bar", "Beauty Salon", "Bowling Alley", "Bus Station", "Cafe", "Campground", "Car Rental", "Casino", "Lodging", "Movie Theater", "Museum", "Night Club", "Park", "Parking", "Restaurant", "Shopping Mall", "Stadium", "Subway Station", "Taxi Stand", "Train Station", "Transit Station", "Travel Agency", "Zoo");

    private EditText editTextKeyword;
    private TextView textViewKeywordError;
    private Spinner spinnerCategory;
    private EditText editTextDistance;
    private PlacesAutocompleteTextView placesAutocompleteTextViewOther;
    private RadioGroup radioGroupLocation;
    private RadioButton radioButtonCurrent;
    private RadioButton radioButtonOther;
    private TextView textViewOtherError;
    private Button buttonSearch;
    private Button buttonClear;

    private FusedLocationProviderClient mFusedLocationClient;

    public SearchFragment() {

    }

    // TODO: Add newInstance method?

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity().getApplicationContext());

        editTextKeyword = view.findViewById(R.id.editTextKeyword);
        textViewKeywordError = view.findViewById(R.id.textViewKeywordError);

        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                getActivity().getApplicationContext(), android.R.layout.simple_spinner_item, CATEGORIES);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner sItems = (Spinner) view.findViewById(R.id.spinnerCategory);
        sItems.setAdapter(spinnerAdapter);

        editTextDistance = view.findViewById(R.id.editTextDistance);
        placesAutocompleteTextViewOther = view.findViewById(R.id.placesAutoCompleteTextViewOther);
        radioButtonOther = view.findViewById(R.id.radioButtonOther);
        radioButtonCurrent = view.findViewById(R.id.radioButtonCurrent);
        radioGroupLocation = view.findViewById(R.id.radioGroupLocation);
        radioGroupLocation.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                switch (checkedId) {
                    case R.id.radioButtonCurrent:
                        placesAutocompleteTextViewOther.setEnabled(false);
                        textViewOtherError.setVisibility(View.GONE);
                        break;
                    case R.id.radioButtonOther:
                        placesAutocompleteTextViewOther.setEnabled(true);
                        break;
                    default:
                        Log.e(TAG, "Unknown radio button selected!");
                }
            }
        });
        textViewOtherError = view.findViewById(R.id.textViewOtherError);

        buttonSearch = view.findViewById(R.id.buttonSearch);
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                nearbySearch(v);
            }
        });

        buttonClear = view.findViewById(R.id.buttonClear);
        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                clearAll(v);
            }
        });


    }

    /**
     * Checks if the keyword field (and the other location, if selected) fields are nonempty, and
     * returns true if they are. Makes any relevant error messages visible.
     *
     * @return
     */
    private boolean validate() {

        boolean isValid = true;
        String keyword = editTextKeyword.getText().toString();
        if (keyword.trim().length() == 0) {
            isValid = false;
            textViewKeywordError.setVisibility(View.VISIBLE);
        } else {
            textViewKeywordError.setVisibility(View.GONE);
        }

        String other = placesAutocompleteTextViewOther.getText().toString();
        if (radioButtonOther.isChecked()) {

            if (other.trim().length() == 0) {
                isValid = false;
                textViewOtherError.setVisibility(View.VISIBLE);
            } else {
                textViewOtherError.setVisibility(View.GONE);
            }
        } else {
            textViewOtherError.setVisibility(View.GONE);
        }

        if (!isValid) {
            Toast.makeText(getActivity().getApplicationContext(), "Please fix all fields with errors", Toast.LENGTH_SHORT).show();
        }

        return isValid;
    }

    /**
     * Search button
     *
     * @param v
     */
    public void nearbySearch(View v) {

        if (validate()) {

            Log.d(TAG, "Starting volley stuff...");



            // Build the request
            buildAndExecuteRequestUrl();

        }
    }

    /**
     * Creates a request URL to the backend
     *
     * @return
     */
    @SuppressLint("MissingPermission")
    private void buildAndExecuteRequestUrl() {

        final Uri.Builder builder = Uri.parse(Constants.URL_NEARBY_SEARCH).buildUpon();

        String keyword = editTextKeyword.getText().toString().trim();
        builder.appendQueryParameter("keyword", keyword);

        String category = spinnerCategory.getSelectedItem().toString().replace(" ", "_").toLowerCase();
        builder.appendQueryParameter("category", category);

        String distanceString = editTextDistance.getText().toString();
        if (distanceString.trim().length() > 0) {
            try {
                float radius = Float.parseFloat(distanceString) * 1609.344f;
                builder.appendQueryParameter("distance", String.valueOf(radius));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Distance must be numeric.");
                Toast.makeText(getActivity().getApplicationContext(), "Distance is non-numeric, ignoring", Toast.LENGTH_SHORT).show();
            }
        }

        if (radioButtonOther.isChecked()) {
            String other = placesAutocompleteTextViewOther.getText().toString();

            // TODO: Location
            final Uri.Builder builderGeocode = Uri.parse(Constants.URL_GEOCODE).buildUpon();
            builderGeocode.appendQueryParameter("address", other);
            String urlGeocode = builderGeocode.build().toString();

            RequestQueue queue = Volley.newRequestQueue(getActivity().getApplicationContext());
            StringRequest stringRequest = new StringRequest(Request.Method.GET, urlGeocode,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String responseString) {

                            Log.v(TAG, "Response is: " + responseString);

                            try {
                                JSONObject locationJSON = new JSONObject(responseString)
                                        .getJSONArray("results")
                                        .getJSONObject(0)
                                        .getJSONObject("geometry")
                                        .getJSONObject("location");

                                String location = locationJSON.getDouble("lat")+","+locationJSON.getDouble("lng");
                                builder.appendQueryParameter("location", location);
                                String url = builder.build().toString();
                                Log.d(TAG, "Generated URL via geocode: " + url);

                                callRequest(url);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {


                    Log.d(TAG, "That didn't work!");
                    Log.e(TAG, error.toString());
                    error.printStackTrace();
                }
            });

            // Add the request to the RequestQueue.
            queue.add(stringRequest);

        }
        else {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            Double lat = 34.007889;
                            Double lng= -118.2585096;

                            if (location != null) {
                                // Logic to handle location object
                                lat = location.getLatitude();
                                lng = location.getLongitude();
                            }
                            Log.e(TAG, "Location is null");
                            builder.appendQueryParameter("location", lat+","+lng);

                            String url = builder.build().toString();
                            Log.d(TAG, "Generated URL via device location: " + url);

                            callRequest(url);
                        }
                    }).addOnFailureListener(getActivity(), new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                    Double lat = 34.007889;
                    Double lng= -118.2585096;
                    Log.e(TAG, "Location detect failed");
                    builder.appendQueryParameter("location", lat+","+lng);

                    String url = builder.build().toString();
                    Log.d(TAG, "Generated URL via device location: " + url);

                    callRequest(url);
                }
            });
        }

    }

    private ProgressDialog dialog;
    private void callRequest(String url) {

        dialog = new ProgressDialog(getActivity());
        dialog.setMessage("Fetching results");
        dialog.show();

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(getActivity().getApplicationContext());

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String responseString) {

                        if(dialog.isShowing()) { dialog.dismiss(); }

                        // Display the first 500 characters of the response string.
                        Log.v(TAG, "Response is: " + responseString);

                        // Start a new activity with the results
                        Intent myIntent = new Intent(getActivity(), ResultsActivity.class);
                        myIntent.putExtra("resultJSON", responseString);
                        startActivity(myIntent);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                if(dialog.isShowing()) { dialog.dismiss(); }

                Log.d(TAG, "That didn't work!");
                Log.e(TAG, error.toString());
                error.printStackTrace();
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    /**
     * Clears text fields and resets form controls.
     *
     * @param v
     */
    public void clearAll(View v) {

        editTextKeyword.setText("");
        editTextDistance.setText("");
        radioGroupLocation.check(R.id.radioButtonCurrent);
        placesAutocompleteTextViewOther.setText("");
        textViewOtherError.setVisibility(View.GONE);
        textViewKeywordError.setVisibility(View.GONE);
        spinnerCategory.setSelection(0);
    }
}