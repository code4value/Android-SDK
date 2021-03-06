/**
 * Copyright 2015 Accela, Inc.
 * <p/>
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to
 * use, copy, modify, and distribute this software in source code or binary
 * form for use in connection with the web services and APIs provided by
 * Accela.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package testcase.accela.com.constructdemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.accela.mobile.AMError;
import com.accela.mobile.AMLogger;
import com.accela.mobile.AMLoginView;
import com.accela.mobile.AMLoginViewDelegate;
import com.accela.mobile.AMRequest;
import com.accela.mobile.AMRequestDelegate;
import com.accela.mobile.AMRequestSender;
import com.accela.mobile.AMSessionDelegate;
import com.accela.mobile.AMSetting;
import com.accela.mobile.AccelaMobile;
import com.accela.mobile.http.AMDocDownloadRequest;
import com.accela.mobile.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AgencyTestActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String APP_ID = "635442545792218073";
    private static final String APP_SECRET = "28c6edc56e714078a23a50a4193f348f";

    private static String SERVICE_URI_RECORD_LIST = "/v4/records/";
    private static String SERVICE_URI_RECORD_SEARCH = "/v4/records/{recordIds}/";
    private static String SERVICE_URI_RECORD_CREATE = "/v4/records/";
    private static String SERVICE_URI_RECORD_AttachmentList = "/v4/records/{recordId}/documents/";
    private static String SERVICE_URI_RECORD_AttachmentUpload = "/v4/records/{recordId}/documents/";
    private static String SERVICE_URI_RECORD_AttachmentDownload = "/v4/documents/{documentId}/download/";
    private static String SERVICE_URI_INSPECTION_LIST = "/v4/inspections/";
    private static String SERVICE_URI_APP_SETTINGS = "/v4/appsettings/";
    private static String[] permissions4Authorization = new String[]{"search_records",
            "get_records", "get_record", "get_record_inspections",
            "get_inspections", "get_inspection", "get_record_documents",
            "get_document", "create_record", "create_record_document",
            "download_document", "create_record_documents",
            "get_gis_settings"};
    private final String URL_SCHEMA = "amtest";
    private final String AGENCY = "ISLANDTON";
    private String recordId = "14CAP-00000-000CT";
    private final int PICK_IMAGE_REQUEST = 1;

    private Button btnAgencyEmbeddedWebLogin, btnGetRecords, btnGetSpecificRecord, btnCreateRecord, btnGetInspections,
            btnDownloadAttachmentList, btnLoadImage, btnDownloadAttachmentWithProgress, btnUploadAttachment,
            btnAppSettings, btnAgencyLogout, btnBack;
    private ArrayList<String> attachmentIds = new ArrayList<String>();
    private AccelaMobile accelaMobile;


    private ProgressDialog accessTokenProgressDialog = null;
    private ProgressDialog progressDialog;
    private ViewGroup mainLayout = null;
    private AMRequest currentRequest = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize UI.
        setContentView(R.layout.activity_agency_test);
        initContentView();
        initAccelaMobile();
        // Register receiver for the broadcast message which will be sent out
        // from AuthorizationManager when user logs in successfully.
        LocalBroadcastManager.getInstance(this).registerReceiver(
                MessageReceiver4LoggedIn,
                new IntentFilter(AMSetting.BROARDCAST_ACTION_LOGGED_IN));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                MessageReceiver4SessionInvaid,
                new IntentFilter(AMSetting.BROARDCAST_ACTION_SESSION_INVALID));
    }


    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, HomeActivity.class));
        this.finish();
        super.onBackPressed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && intent != null) {
                if (intent.getData() != null) {
                    String filePath = this.getRealPathFromURI(this, intent.getData());
                    String servicePath = SERVICE_URI_RECORD_AttachmentUpload.replace(
                            "{recordId}", recordId);
                    JSONArray fileInfoJsonArray = new JSONArray();
                    Map<String, String> fileInformationMap = new HashMap<String, String>();
                    addDocumentToUpload(filePath, fileInfoJsonArray, fileInformationMap);
                    // Populate the post data.
                    String fileInfoJsonArrayStr = fileInfoJsonArray.toString();
                    RequestParams postParams = new RequestParams();
                    postParams.put("fileInfo", fileInfoJsonArrayStr);
                    // Invoke cloud API to upload the files.
                    currentRequest = accelaMobile.getRequestSender().uploadAttachments(servicePath, null, postParams,
                            fileInformationMap, null, uploadDocumentRequestDelegate);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PICK_IMAGE_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.selectImage();
            }
        }
    }

    public void onClick(View view) {

        String servicePath = null;
        RequestParams requestParams = new RequestParams();
        AMRequestSender request = this.accelaMobile.getRequestSender();

        // Define record Id for a specific record
        switch (view.getId()) {
            case R.id.btnAgencyEmbeddedWebLogin:
                this.accelaMobile.getAuthorizationManager().showAuthorizationWebView(this, permissions4Authorization, URL_SCHEMA, AGENCY);
                break;
            case R.id.btnAgencyGetRecords:
                if (isSessionValid()) {
                    servicePath = SERVICE_URI_RECORD_LIST;
                    requestParams.put("limit", "10");
                    requestParams.put("offset", "0");
                    requestParams.put("openedDateRange",
                            getDateRangeBeforeAndAfter(7, 7)); // From 7 days before
                    // to 7 days after
                    currentRequest = request.sendRequest(servicePath,
                            requestParams, null, recordListRequestDelegate);
                }
                break;
            case R.id.btnAgencyGetSpecificRecord:
                if (isSessionValid()) {
                    servicePath = SERVICE_URI_RECORD_SEARCH.replace("{recordIds}",
                            recordId);
                    currentRequest = request.sendRequest(servicePath, null, null,
                            requestDelegate);
                }
                break;
            case R.id.btnAgencyCreateRecord:
                if (isSessionValid()) {
                    servicePath = SERVICE_URI_RECORD_CREATE;

                    JSONObject recordJson = populateRecordJson();
                    RequestParams postData = new RequestParams(recordJson);
                    currentRequest = request.sendRequest(servicePath, null, null,
                            AMRequest.HTTPMethod.POST, postData, requestDelegate);
                }
                break;
            case R.id.btnAgencyGetInspections:
                if (isSessionValid()) {
                    servicePath = SERVICE_URI_INSPECTION_LIST;
                    Date dateToday = new Date();
                    Date scheduledDateTo = dateToday;
                    Calendar now = Calendar.getInstance();
                    now.setTime(dateToday);
                    now.set(Calendar.DATE, now.get(Calendar.DATE) - 600);
                    Date scheduledDateFrom = now.getTime();

                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    // search inspections of 600 days before till now
                    requestParams.put("scheduledDateFrom",
                            dateFormat.format(scheduledDateFrom));
                    requestParams.put("scheduledDateTo",
                            dateFormat.format(scheduledDateTo));
                    // requestParams.put("inspectorIds", inspectorId);
                    requestParams.put("limit", "10");
                    requestParams.put("offset", "0");
                    // asynchronous request, put requestDelegate for handling
                    // results
                    currentRequest = request.sendRequest(servicePath,
                            requestParams, null, requestDelegate);
                }
                break;

            case R.id.btnAgencyUploadAttachment:
                if (isSessionValid() && this.requestReadExternalStorage()) {
                    this.selectImage();
                }
                break;
            case R.id.btnAgencyDownloadAttachmentList:
                if (isSessionValid()) {
                    servicePath = SERVICE_URI_RECORD_AttachmentList.replace(
                            "{recordId}", recordId);
                    requestParams.put("limit", "10");
                    requestParams.put("offset", "0");
                    ProgressDialog waitingView = ProgressDialog.show(
                            this,
                            null,
                            this.getResources().getString(
                                    R.string.msg_request_being_processed), true,
                            true);
                    currentRequest = request.sendRequest(servicePath,
                            requestParams, null, attachmentListRequestDelegate);
                    waitingView.dismiss();
                }
                break;
            case R.id.btnAgencyDownloadAttachmentWithProgress:
                if (isSessionValid()) {
                    // Get an attachment ID if attachment list has been requested.
                    if (this.attachmentIds.size() > 0) {
                        String attachmentId = this.attachmentIds.get(0);

                        servicePath = SERVICE_URI_RECORD_AttachmentDownload
                                .replace("{documentId}", attachmentId);
                        String downloadFilePath = this.getApplicationContext()
                                .getFilesDir().getAbsolutePath()
                                + "/AccelaAnalytics.png";
                        currentRequest = request.downloadAttachment(
                                servicePath, null, null, downloadFilePath,
                                downloadDocumentRequestDelegate);
                    } else {
                        Toast toast = Toast.makeText(this,
                                "Please get attachment list first.",
                                Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }
                break;
            case R.id.btnAgencyLoadAttachmentList:
                if (isSessionValid()) {
                    // Get an attachment ID if attachment list has been requested.
                    if (this.attachmentIds.size() > 0) {
                        String attachmentId = this.attachmentIds.get(0);

                        servicePath = SERVICE_URI_RECORD_AttachmentDownload
                                .replace("{documentId}", attachmentId);
                        currentRequest = request.loadImage(servicePath, null, null, loadDocumentRequestDelegate);
                    } else {
                        Toast toast = Toast.makeText(this,
                                "Please get attachment list first.",
                                Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }
                break;
            case R.id.btnAppSettings:
                servicePath = SERVICE_URI_APP_SETTINGS;
                HashMap<String, String> customHttpHeader = new HashMap<String, String>();
                customHttpHeader.put("x-accela-appid", APP_ID);
                customHttpHeader.put("x-accela-appsecret", APP_SECRET);

                requestParams = new RequestParams();
                currentRequest = request.sendRequest(servicePath,
                        requestParams, customHttpHeader, requestDelegate);
                break;
            case R.id.btnAgencyLogout:
                if (isSessionValid()) {
                    accelaMobile.getAuthorizationManager().logout();
                }
                break;
            case R.id.btnAgencyBack:
                this.onBackPressed();
        }
    }

    private void initContentView() {
        // Initialize UI elements.
        this.mainLayout = (LinearLayout) this
                .findViewById(R.id.layoutAgencyTestMainView);
        this.btnAgencyEmbeddedWebLogin = (Button) this
                .findViewById(R.id.btnAgencyEmbeddedWebLogin);

        this.btnAgencyLogout = (Button) this.findViewById(R.id.btnAgencyLogout);
        this.btnGetRecords = (Button) this
                .findViewById(R.id.btnAgencyGetRecords);
        this.btnGetSpecificRecord = (Button) this
                .findViewById(R.id.btnAgencyGetSpecificRecord);
        this.btnGetInspections = (Button) this
                .findViewById(R.id.btnAgencyGetInspections);
        this.btnCreateRecord = (Button) this
                .findViewById(R.id.btnAgencyCreateRecord);
        this.btnDownloadAttachmentList = (Button) this
                .findViewById(R.id.btnAgencyDownloadAttachmentList);
        this.btnDownloadAttachmentWithProgress = (Button) this
                .findViewById(R.id.btnAgencyDownloadAttachmentWithProgress);
        this.btnUploadAttachment = (Button) this
                .findViewById(R.id.btnAgencyUploadAttachment);
        this.btnAppSettings = (Button) this.findViewById(R.id.btnAppSettings);

        this.btnBack = (Button) this.findViewById(R.id.btnAgencyBack);
        this.btnLoadImage = (Button) this.findViewById(R.id.btnAgencyLoadAttachmentList);

        // Set events for buttons.
        this.btnAgencyEmbeddedWebLogin.setOnClickListener(this);
        this.btnAgencyLogout.setOnClickListener(this);
        this.btnGetRecords.setOnClickListener(this);
        this.btnGetSpecificRecord.setOnClickListener(this);
        this.btnCreateRecord.setOnClickListener(this);
        this.btnGetInspections.setOnClickListener(this);
        this.btnDownloadAttachmentList.setOnClickListener(this);
        this.btnDownloadAttachmentWithProgress.setOnClickListener(this);
        this.btnUploadAttachment.setOnClickListener(this);
        this.btnAppSettings.setOnClickListener(this);
        this.btnBack.setOnClickListener(this);
        this.btnLoadImage.setOnClickListener(this);
    }

    private void initAccelaMobile() {
        // Create an AccelaMobile instance with the App ID and App Secret of the
        // registered app.
        this.accelaMobile = AccelaMobile.getInstance();
        this.accelaMobile.initialize(getApplicationContext(), APP_ID, APP_SECRET, AccelaMobile.Environment.PROD, sessionDelegate);
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        this.startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private Dialog createImagePreviewDialog(Bundle bundle) {
        String localPath = bundle.getString("localPath");
        ViewGroup.LayoutParams mainLayoutParams = mainLayout.getLayoutParams();
        FrameLayout.LayoutParams viewLayoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        LinearLayout photoLayout = new LinearLayout(this);
        photoLayout.setOrientation(LinearLayout.VERTICAL);
        photoLayout.setLayoutParams(viewLayoutParams);
        final Dialog photoPreviewDialog = new Dialog(this);
        photoPreviewDialog.setTitle(this.getResources().getString(
                R.string.image_preview_title));

        String extendsion = localPath.substring(localPath.lastIndexOf(".") + 1,
                localPath.length()).toLowerCase(Locale.US);
        Boolean isImageFile = extendsion.equals("jpg")
                || extendsion.equals("gif") || extendsion.equals("png")
                || extendsion.equals("jpeg") || extendsion.equals("bmp");
        File file = new File(localPath);
        if (file.exists() && isImageFile) {
            ImageView imageView = new ImageView(this);
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(file);
                imageView.setImageBitmap(BitmapFactory
                        .decodeStream(fileInputStream));
                AMLogger.logInfo("File's length = %d, fileInputStream = %s",
                        file.length(), fileInputStream.toString());
            } catch (FileNotFoundException e) {
                AMLogger.logError(
                        "Failed to load the image because of non-existent file path or invalid image extension.\nFile path: %s",
                        localPath);
            }
            imageView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    photoPreviewDialog.dismiss();
                }
            });
            photoLayout.addView(imageView, viewLayoutParams);
        } else {
            TextView messageTextView = new TextView(this);
            messageTextView.setBackgroundColor(Color.YELLOW);
            messageTextView.setText(this.getResources().getString(
                    R.string.error_image_preview));
            photoLayout.addView(messageTextView, viewLayoutParams);
        }

        photoPreviewDialog.addContentView(photoLayout, mainLayoutParams);

        return photoPreviewDialog;
    }

    // Check whether the current user session is valid or not.
    private boolean isSessionValid() {
        Boolean isValid = (accelaMobile != null)
                && (accelaMobile.isSessionValid());
        if (!isValid) {
            Toast toast = Toast.makeText(this,
                    this.getResources().getString(R.string.msg_not_logged_in),
                    Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
        return isValid;
    }

    private void addDocumentToUpload(String localImagePath,
                                     JSONArray fileInfoJsonArray, Map<String, String> fileInformationMap) {
        String fileName = localImagePath.substring(localImagePath.lastIndexOf("/")+1);
        String fileType = fileName.substring(fileName.lastIndexOf(".") + 1,
                fileName.length()).toLowerCase(Locale.US);
        String applicationImagePath = this.getApplicationContext()
                .getFilesDir().getAbsolutePath()
                + "/" + fileName;
        copyImageFile2App(localImagePath, applicationImagePath);
        // Populate parameters for file binary stream part and Json data part to
        // be posted
        // Add a document to be upload.
        JSONObject fileInfoJson = new JSONObject();
        try {
            fileInfoJson.put("serviceProviderCode",
                    accelaMobile.getAgency());
            fileInfoJson.put("fileName", fileName);
            fileInfoJson.put("type", fileType);
            fileInfoJson.put("description", "Upload document for testing.");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        fileInfoJsonArray.put(fileInfoJson);
        fileInformationMap.put(fileName, applicationImagePath);
    }

    // Copy image file from resource folder to application folder.
    private void copyImageFile2App(String localImagePath, String applicationImagePath) {
        // Create the image file under application's directory before uploading
        // it
        File file = new File(localImagePath);
        int imageFileSize = 0;
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            imageFileSize = inputStream.available();
            // Create the image file in application directory.
            int read = 0;
            byte[] bytes = new byte[imageFileSize];
            OutputStream outputStream = null;
            outputStream = new FileOutputStream(new File(applicationImagePath));
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            outputStream.flush();
            inputStream.close();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    // Get date range, then return string looks like "20130913-20130930"
    private String getDateRangeBeforeAndAfter(int beforeDays, int afterDays) {
        Calendar calendar1 = Calendar.getInstance();
        // Calculate the current date.
        int year = calendar1.get(Calendar.YEAR);
        int month = calendar1.get(Calendar.MONTH) + 1;
        int day = calendar1.get(Calendar.DAY_OF_MONTH);
        String yearString = String.valueOf(year);
        String monthString = String.valueOf(month);
        if (month < 10)
            monthString = "0" + monthString;
        String dayString = String.valueOf(day);
        if (day < 10)
            dayString = "0" + dayString;
        // Calculate the from date.
        calendar1.set(Calendar.DATE, calendar1.get(Calendar.DATE) - beforeDays);
        year = calendar1.get(Calendar.YEAR);
        month = calendar1.get(Calendar.MONTH) + 1;
        day = calendar1.get(Calendar.DAY_OF_MONTH);
        yearString = String.valueOf(year);
        monthString = String.valueOf(month);
        if (month < 10)
            monthString = "0" + monthString;
        dayString = String.valueOf(day);
        if (day < 10)
            dayString = "0" + dayString;
        String fromDateString = yearString + monthString + dayString;

        // Calculate the to date.
        Calendar calendar2 = Calendar.getInstance();
        calendar2.set(Calendar.DATE, calendar2.get(Calendar.DATE) + afterDays);
        year = calendar2.get(Calendar.YEAR);
        month = calendar2.get(Calendar.MONTH) + 1;
        day = calendar2.get(Calendar.DAY_OF_MONTH);
        yearString = String.valueOf(year);
        monthString = String.valueOf(month);
        if (month < 10)
            monthString = "0" + monthString;
        dayString = String.valueOf(day);
        if (day < 10)
            dayString = "0" + dayString;
        String toDateString = yearString + monthString + dayString;

        // Return a string like 20130530-20130531
        return fromDateString + "-" + toDateString;
    }

    // Create and show an alert dialog
    private void createAlertDialog(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                }).create().show();
    }

    private void dismissProgressDialog() {
        // Dismiss the process waiting view
        if ((progressDialog != null) && (progressDialog.isShowing())) {
            progressDialog.dismiss();
        }
    }

    private void showProgressDialog(String message) {
        progressDialog = ProgressDialog.show(AgencyTestActivity.this, null, message);
    }

    // Session delegate for Accela Mobile
    private AMLoginViewDelegate loginDialogDelegate = new AMLoginViewDelegate() {
        @Override
        public void amDialogFetch(AMLoginView loginView) {
            Log.e("login", "fetcing");
            accessTokenProgressDialog = ProgressDialog.show(
                    AgencyTestActivity.this, null,
                    AgencyTestActivity.this.getString(R.string.msg_login),
                    true, true);
        }

        @Override
        public void amDialogLogin(AMLoginView loginView) {
            Log.e("login", "login");
            if ((accessTokenProgressDialog != null)
                    && (accessTokenProgressDialog.isShowing())) {
                accessTokenProgressDialog.dismiss();
            }
        }

        @Override
        public void amDialogNotLogin(boolean cancelled) {
            Log.e("login", "not login");
            if ((accessTokenProgressDialog != null)
                    && (accessTokenProgressDialog.isShowing())) {
                accessTokenProgressDialog.dismiss();
            }

        }

        @Override
        public void amDialogLoginFailure(AMError error) {
            if ((accessTokenProgressDialog != null)
                    && (accessTokenProgressDialog.isShowing())) {
                accessTokenProgressDialog.dismiss();
            }
        }
    };

    // Session delegate for Accela Mobile
    private AMSessionDelegate sessionDelegate = new AMSessionDelegate() {
        public void amDidLogin() {
            Log.e("login", "session amDidLogin");
            // Dismiss progress dialog.
            if ((accessTokenProgressDialog != null)
                    && (accessTokenProgressDialog.isShowing())) {
                accessTokenProgressDialog.dismiss();
            }
            // Show message.
            Toast.makeText(
                    AgencyTestActivity.this,
                    AgencyTestActivity.this.getResources().getString(
                            R.string.msg_logged_in), Toast.LENGTH_SHORT).show();
            AMLogger.logInfo("In AgencyTestActivity: Session Delegate.amDidLogin() invoked...");
        }

        public void amDidLoginFailure(AMError error) {
            Log.e("login", "session amDidLoginFailure");
            // Dismiss progress dialog.
            if ((accessTokenProgressDialog != null)
                    && (accessTokenProgressDialog.isShowing())) {
                accessTokenProgressDialog.dismiss();
            }
            // Show message.
            Toast.makeText(
                    AgencyTestActivity.this,
                    AgencyTestActivity.this.getResources().getString(
                            R.string.msg_login_failed), Toast.LENGTH_SHORT)
                    .show();
            AMLogger.logInfo(
                    "In AgencyTestActivity: Session Delegate.amDidLoginFailure() invokded: %s",
                    error.toString());
        }

        public void amDidCancelLogin() {
            Log.e("login", "session amDidCancelLogin");
            AMLogger.logInfo("In AgencyTestActivity: Session Delegate.amDidCancelLogin() invoked...");
        }

        public void amDidSessionInvalid(AMError error) {
            Log.e("login", "session amDidSessionInvalid");
            AMLogger.logInfo(
                    "In AgencyTestActivity: Session Delegate.amDidSessionInvalid() invoked: %s",
                    error.toString());
        }

        public void amDidLogout() {
            Log.e("login", "session amDidLogout");
            Toast.makeText(
                    AgencyTestActivity.this,
                    AgencyTestActivity.this.getResources().getString(
                            R.string.msg_logged_out), Toast.LENGTH_SHORT)
                    .show();
            AMLogger.logInfo("In AgencyTestActivity: Session Delegate.amDidLogout() invoked...");
        }
    };

    // The following delegates are created for the above service requests
    private AMRequestDelegate requestDelegate = new AMRequestDelegate() {
        @Override
        public void onStart() {
            amRequestStarted(currentRequest);
            // Show progress waiting view
            showProgressDialog(AgencyTestActivity.this.getResources().getString(
                    R.string.msg_request_being_processed));
        }

        @Override
        public void onSuccess(JSONObject responseJson) {
            amRequestDidReceiveResponse(currentRequest);
            amRequestDidLoad(currentRequest, responseJson);
            dismissProgressDialog();
            // Show dialog with the retrured Json data
            createAlertDialog(
                    AgencyTestActivity.this.getResources().getString(
                            R.string.msg_request_completed_title),
                    AgencyTestActivity.this.getResources().getString(
                            R.string.msg_request_completed_message)
                            + ": \n " + responseJson.toString());
            Log.d("info", responseJson.toString());
        }

        @Override
        public void onFailure(AMError error) {
            amRequestDidReceiveResponse(currentRequest);
            // Dismiss the process waiting view
            dismissProgressDialog();
            AMError amError = new AMError(error.getStatus(), errorMessage,
                    traceId, null, null);
            // Show dialog with the returned error
            createAlertDialog(
                    AgencyTestActivity.this.getResources().getString(
                            R.string.error_request_failed_title),
                    amError.toString());
        }
    };

    private AMRequestDelegate recordListRequestDelegate = new AMRequestDelegate() {
        @Override
        public void onStart() {
            amRequestStarted(currentRequest);
            // Show progress waiting view
            showProgressDialog(AgencyTestActivity.this.getResources().getString(R.string.msg_request_being_processed));
        }

        @Override
        public void onSuccess(JSONObject responseJson) {
            amRequestDidReceiveResponse(currentRequest);
            this.amRequestDidLoad(currentRequest, responseJson);
            JSONArray recordsArray = responseJson.optJSONArray("result");
            dismissProgressDialog();
            // Show dialog with the returned Json data
            if ((recordsArray != null) && (recordsArray.length() > 0)) {
                createAlertDialog(
                        AgencyTestActivity.this.getResources().getString(
                                R.string.msg_request_completed_title),
                        AgencyTestActivity.this.getResources().getString(
                                R.string.msg_request_completed_message)
                                + ": \n" + recordsArray.toString());
            } else {
                createAlertDialog(
                        AgencyTestActivity.this.getResources().getString(
                                R.string.error_request_failed_title),
                        AgencyTestActivity.this.getResources().getString(
                                R.string.error_request_failed_message)
                                + ": \n" + responseJson.toString());
            }
        }

        @Override
        public void onFailure(AMError error) {
            amRequestDidReceiveResponse(currentRequest);
            // Dismiss the process waiting view
            dismissProgressDialog();
            AMError amError = new AMError(error.getStatus(), errorMessage,
                    traceId, null, null);
            this.amRequestDidFailWithError(currentRequest, amError);
            // Show dialog with the returned error
            createAlertDialog(
                    AgencyTestActivity.this.getResources().getString(
                            R.string.error_request_failed_title),
                    amError.toString());
        }
    };

    private AMRequestDelegate attachmentListRequestDelegate = new AMRequestDelegate() {
        @Override
        public void onStart() {
            amRequestStarted(currentRequest);
            // Show progress waiting view
            showProgressDialog(AgencyTestActivity.this.getResources().getString(R.string.msg_request_being_processed));
        }

        @Override
        public void onSuccess(JSONObject responseJson) {
            amRequestDidReceiveResponse(currentRequest);
            amRequestDidLoad(currentRequest, responseJson);

            JSONArray attachmentsArray = responseJson.optJSONArray("result");
            if ((attachmentsArray != null) && (attachmentsArray.length() > 0)) {
                AgencyTestActivity.this.attachmentIds = new ArrayList<String>();

                for (int i = 0; i < attachmentsArray.length(); i++) {
                    String attachmentId = null;
                    try {
                        attachmentId = attachmentsArray.getJSONObject(i)
                                .getString("id");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (attachmentId != null) {
                        AgencyTestActivity.this.attachmentIds.add(attachmentId);
                    }
                }
            }

            dismissProgressDialog();
            // Show dialog with the returned Json data
            if ((attachmentsArray != null) && (attachmentsArray.length() > 0)) {
                createAlertDialog(
                        AgencyTestActivity.this.getResources().getString(
                                R.string.msg_request_completed_title),
                        AgencyTestActivity.this.getResources().getString(
                                R.string.msg_request_completed_message)
                                + ": \n" + attachmentsArray.toString());
            } else {
                createAlertDialog(
                        AgencyTestActivity.this.getResources().getString(
                                R.string.error_request_failed_title),
                        AgencyTestActivity.this.getResources().getString(
                                R.string.error_request_failed_message)
                                + ": \n" + responseJson.toString());
            }
        }

        @Override
        public void onFailure(AMError error) {
            amRequestDidReceiveResponse(currentRequest);
            // Dismiss the process waiting view
            dismissProgressDialog();
            AMError amError = new AMError(error.getStatus(), errorMessage,
                    traceId, null, null);
            this.amRequestDidFailWithError(currentRequest, amError);
            // Show dialog with the returned error
            createAlertDialog(
                    AgencyTestActivity.this.getResources().getString(
                            R.string.error_request_failed_title),
                    amError.toString());
        }
    };

    private AMDocDownloadRequest.AMDownloadDelegate downloadDocumentRequestDelegate
            = new AMDocDownloadRequest.AMDownloadDelegate() {
        @Override
        public void onStart() {
            // Show progress waiting view
            showProgressDialog(AgencyTestActivity.this.getResources().getString(R.string.msg_request_being_processed));
        }

        @Override
        public void onSuccess(File file) {

            String localFilePath = file.getAbsolutePath();
            Bundle dataBundle = new Bundle();
            dataBundle.putString("localPath", localFilePath);
            dismissProgressDialog();
            // Show the photo preview in pop-up dialog
            Dialog photoPreviewDialog = AgencyTestActivity.this
                    .createImagePreviewDialog(dataBundle);
            photoPreviewDialog.show();
        }

        @Override
        public void onFailure(AMError error) {
            // Dismiss the process waiting view
            dismissProgressDialog();

            // Show dialog with the returned error
            createAlertDialog(
                    AgencyTestActivity.this.getResources().getString(
                            R.string.error_request_failed_title),
                    error.toString());
        }
    };

    private AMRequestDelegate loadDocumentRequestDelegate = new AMRequestDelegate() {
        @Override
        public void onStart() {
            // Show progress waiting view
            showProgressDialog(AgencyTestActivity.this.getResources().getString(R.string.msg_request_being_processed));
        }

        public void onSuccess(Bitmap bitmap) {
            dismissProgressDialog();
            Toast.makeText(AgencyTestActivity.this, "load Image success", Toast.LENGTH_SHORT).show();
        }


        @Override
        public void onFailure(AMError error) {
            // Dismiss the process waiting view
            dismissProgressDialog();

            // Show dialog with the returned error
            createAlertDialog(
                    AgencyTestActivity.this.getResources().getString(
                            R.string.error_request_failed_title),
                    error.toString());
        }
    };

    private AMRequestDelegate uploadDocumentRequestDelegate = new AMRequestDelegate() {
        @Override
        public void onStart() {
            amRequestStarted(currentRequest);
            // Show progress waiting view
            showProgressDialog(AgencyTestActivity.this.getResources().getString(R.string.msg_request_being_processed));
        }

        @Override
        public void onSuccess(JSONObject responseJson) {
            amRequestDidReceiveResponse(currentRequest);
            // Dismiss the process waiting view
            dismissProgressDialog();
            // Show dialog with the returned data
            createAlertDialog(
                    AgencyTestActivity.this.getResources().getString(
                            R.string.msg_request_completed_title),
                    AgencyTestActivity.this.getResources().getString(
                            R.string.msg_request_completed_message)
                            + ": \n" + responseJson);
        }

        @Override
        public void onFailure(AMError error) {
            amRequestDidReceiveResponse(currentRequest);
            // Dismiss the process waiting view
            dismissProgressDialog();
            // There is a special case for document uploading because the API
            // won't return JSON.
            // So we display error dialog only when both trace ID and error
            // message are not null.
            boolean isUploadingSuccessful = (traceId != null)
                    && (errorMessage == null);
            if (isUploadingSuccessful) {
                createAlertDialog(
                        AgencyTestActivity.this.getResources().getString(
                                R.string.msg_request_completed_title),
                        AgencyTestActivity.this.getResources().getString(
                                R.string.msg_request_completed_message)
                                + ": \n" + "{}");
            } else {
                AMError amError = new AMError(error.getStatus(), errorMessage,
                        traceId, null, null);
                createAlertDialog(AgencyTestActivity.this.getResources()
                                .getString(R.string.error_request_failed_title),
                        amError.toString());
            }
        }
    };

    private JSONObject populateRecordJson() {
        // Contact data
        String contactPhonesJsonStr = "[\"(801) 879-3789\"]";
        String contactRoleJsonStr = "{\"id\":\"Applicant\",\"display\":\"Applicant\"}";
        String contactMailingAddressJsonStr = "{\"streetName\":\"2633 Camino Ramon\",\"unit\":\"Suite 120\",\"city\":\"San Ramon\",\"state\":\"CA\",\"postalCode\":\"94583\"}";
        JSONObject contactJson = new JSONObject();
        try {
            contactJson.put("entityState", "Added");
            contactJson.put("givenName", "Kris");
            contactJson.put("faimilyName", "Trujillo");
            contactJson.put("tels", new JSONArray(contactPhonesJsonStr));
            contactJson.put("contactRole", new JSONObject(contactRoleJsonStr));
            contactJson.put("mainlingAddress", new JSONObject(
                    contactMailingAddressJsonStr));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // ASI data
        String asiItemJsonStr = "[{\"entityState\":\"Added\",\"display\":\"Trip Name\",\"id\":\"Trip Name\",\"value\":\"My Trip\"}]";
        JSONObject asiItemJson = new JSONObject();
        JSONObject asiSubgroupJson = new JSONObject();
        JSONObject asiJson = new JSONObject();
        try {
            asiItemJson.put("items", new JSONArray(asiItemJsonStr));
            asiSubgroupJson.put("subGroups", new JSONArray().put(asiItemJson));
            asiSubgroupJson.put("action", "Added");
            asiSubgroupJson.put("display", "STANDARD");
            asiSubgroupJson.put("id", "STANDARD");
            asiJson.put("asis", new JSONArray().put(asiSubgroupJson));
            asiJson.put("display", "LIC_FISH_RPT");
            asiJson.put("id", "LIC_FISH_RPT");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // ASI Table data
        String column1JsonStr = "{\"entityState\":\"Added\",\"display\":\"Trip Name\",\"id\":\"Trip Name\",\"name\":\"Trip Name\",\"type\":\"String\"}";
        String column2JsonStr = "{\"entityState\":\"Added\",\"display\":\"Trip Location\",\"id\":\"Trip Location\",\"name\":\"Trip Location\",\"type\":\"String\"}";
        String column3JsonStr = "{\"entityState\":\"Added\",\"display\":\"Trip Start Date\",\"id\":\"Trip Start Date\",\"name\":\"Trip Start Date\",\"type\":\"String\"}";
        String column4JsonStr = "{\"entityState\":\"Added\",\"display\":\"Trip End Date\",\"id\":\"Trip End Date\",\"name\":\"Trip End Date\",\"type\":\"String\"}";
        String column5JsonStr = "{\"entityState\":\"Added\",\"display\":\"Fish Type\",\"id\":\"Fish Type\",\"name\":\"Fish Type\",\"type\":\"String\"}";
        String column6JsonStr = "{\"entityState\":\"Added\",\"display\":\"Fish Weight\",\"id\":\"Fish Weight\",\"name\":\"Fish Weight\",\"type\":\"String\"}";
        String column7JsonStr = "{\"entityState\":\"Added\",\"display\":\"Fish Length\",\"id\":\"Fish Length\",\"name\":\"Fish Length\",\"type\":\"String\"}";
        String column8JsonStr = "{\"entityState\":\"Added\",\"display\":\"Fish Count\",\"id\":\"Fish Count\",\"name\":\"Fish Count\",\"type\":\"String\"}";
        String column9JsonStr = "{\"entityState\":\"Added\",\"display\":\"Fish Location\",\"id\":\"Fish Location\",\"name\":\"Fish Location\",\"type\":\"String\"}";
        String value1JsonStr = "{\"entityState\":\"Added\",\"id\":\"Trip Name\",\"value\":\"My Trip\"}";
        String value2JsonStr = "{\"entityState\":\"Added\",\"id\":\"Trip Location\",\"value\":\"-111.000235, 33.000000\"}";
        String value3JsonStr = "{\"entityState\":\"Added\",\"id\":\"Trip Start Date\",\"value\":\"2/1/2012\"}";
        String value4JsonStr = "{\"entityState\":\"Added\",\"id\":\"Trip End Date\",\"value\":\"2/29/2012\"}";
        String value5JsonStr = "{\"entityState\":\"Added\",\"id\":\"Fish Type\",\"value\":\"Paddlefish\"}";
        String value6JsonStr = "{\"entityState\":\"Added\",\"id\":\"Fish Weight\",\"value\":\"5 lbs\"}";
        String value7JsonStr = "{\"entityState\":\"Added\",\"id\":\"Fish Length\",\"value\":\"40 inches\"}";
        String value8JsonStr = "{\"entityState\":\"Added\",\"id\":\"Fish Count\",\"value\":\"1\"}";
        String value9JsonStr = "{\"entityState\":\"Added\",\"id\":\"Fish Location\",\"value\":\"-111.00000,-33.00000\"}";

        JSONArray columnsArrayJson = new JSONArray();
        JSONArray valuesArrayJson = new JSONArray();

        JSONObject asitRowJson = new JSONObject();
        JSONObject asitJson = new JSONObject();

        try {
            columnsArrayJson.put(new JSONObject(column1JsonStr));
            columnsArrayJson.put(new JSONObject(column2JsonStr));
            columnsArrayJson.put(new JSONObject(column3JsonStr));
            columnsArrayJson.put(new JSONObject(column4JsonStr));
            columnsArrayJson.put(new JSONObject(column5JsonStr));
            columnsArrayJson.put(new JSONObject(column6JsonStr));
            columnsArrayJson.put(new JSONObject(column7JsonStr));
            columnsArrayJson.put(new JSONObject(column8JsonStr));
            columnsArrayJson.put(new JSONObject(column9JsonStr));

            valuesArrayJson.put(new JSONObject(value1JsonStr));
            valuesArrayJson.put(new JSONObject(value2JsonStr));
            valuesArrayJson.put(new JSONObject(value3JsonStr));
            valuesArrayJson.put(new JSONObject(value4JsonStr));
            valuesArrayJson.put(new JSONObject(value5JsonStr));
            valuesArrayJson.put(new JSONObject(value6JsonStr));
            valuesArrayJson.put(new JSONObject(value7JsonStr));
            valuesArrayJson.put(new JSONObject(value8JsonStr));
            valuesArrayJson.put(new JSONObject(value9JsonStr));

            asitRowJson.put("values", valuesArrayJson);
            asitRowJson.put("action", "Add");
            asitRowJson.put("display", "STANDARD");
            asitRowJson.put("id", "STANDARD");

            asitJson.put("columns", columnsArrayJson);
            asitJson.put("rows", new JSONArray().put(asitRowJson));
            asitJson.put("display", "LIC_FISH_RPT/STANDARD");
            asitJson.put("id", "LIC_FISH_RPT");
            asitJson.put("subId", "STANDARD");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Record Basic data
        JSONObject recordJson = new JSONObject();
        JSONObject recordTypeJson = new JSONObject();
        try {
            recordTypeJson.put("id", "ServiceRequest-Graffiti-Graffiti-NA");
            recordTypeJson.put("value", "ServiceRequest/Graffiti/Graffiti/NA");
            recordJson.put("type", recordTypeJson);
            recordJson.put("text", "Create from Catch Report");
            // recordJson.put("contacts", new JSONArray().put(contactJson));
            // recordJson.put("additionalInfo", new JSONArray().put(asiJson));
            // recordJson.put("additionalTableInfo", new
            // JSONArray().put(asitJson));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return recordJson;
    }

    private String getRealPathFromURI(Context context, Uri uri){
        if (Build.VERSION.SDK_INT < 19) {
            String[] proj = { MediaStore.Images.Media.DATA };
            String result = null;

            CursorLoader cursorLoader = new CursorLoader(context, uri, proj, null, null, null);
            Cursor cursor = cursorLoader.loadInBackground();
            if(cursor != null){
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                result = cursor.getString(column_index);
            }
            return result;
        } else {
            String filePath = "";
            String wholeID = DocumentsContract.getDocumentId(uri);

            // Split at colon, use second item in the array
            String id = wholeID.split(":")[1];

            String[] column = {MediaStore.Images.Media.DATA};
            // where id is equal to
            String sel = MediaStore.Images.Media._ID + "=?";

            Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, column, sel, new String[]{id}, null);
            int columnIndex = cursor.getColumnIndex(column[0]);
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
            return filePath;
        }
    }

    public boolean requestReadExternalStorage(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //only android 6 above
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PICK_IMAGE_REQUEST);
                return false;
            }
        }
        return true;
    }

    private BroadcastReceiver MessageReceiver4LoggedIn = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the broadcasted Intent
            if (intent.hasExtra("accessToken")) {
                String loggedInUser = intent.getStringExtra("user");
                String accessToken = intent.getStringExtra("accessToken");
                AMLogger.logInfo(
                        "AgencyTestActivity received broadcast [User logged in] for user [%s] with token [%s]",
                        loggedInUser, accessToken);

            }
        }
    };

    private BroadcastReceiver MessageReceiver4SessionInvaid = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the broadcasted Intent
            String loggedInUser = intent.getStringExtra("user");
            AMLogger.logInfo(
                    "AgencyTestActivity received broadcast [Session became invalid] for user [%s]",
                    loggedInUser);
        }
    };

}