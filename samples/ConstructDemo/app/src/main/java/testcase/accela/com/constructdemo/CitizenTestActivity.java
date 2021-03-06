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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.accela.mobile.AMError;
import com.accela.mobile.AMLogger;
import com.accela.mobile.AMRequest;
import com.accela.mobile.AMRequest.HTTPMethod;
import com.accela.mobile.AMRequestDelegate;
import com.accela.mobile.AMRequestSender;
import com.accela.mobile.AMSessionDelegate;
import com.accela.mobile.AccelaMobile;
import com.accela.mobile.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CitizenTestActivity extends AppCompatActivity implements OnClickListener {
    private final String SERVICE_URI_RECORD_LIST = "/v4/records/";
    private final String SERVICE_URI_RECORD_CREATE = "/v4/records/";
    private final String SERVICE_URI_RECORD_SEARCH = "/v4/search/records/";
    private final String AppID = "635442545965802935";
    private final String AppSecret = "e7b22310882f4e5185c9ca339aa1a67c";
    private final String[] Permissions4Authorization = new String[]{"search_records",
            "get_records", "get_record", "get_record_inspections",
            "get_inspections", "get_inspection", "get_record_documents",
            "get_document", "create_record", "create_record_document",
            "a311citizen_create_record"};
    private final String URL_SCHEMA = "amtest";
    private final String AGENCY = "ISLANDTON";

    private ProgressDialog accessTokenProgressDialog = null;
    private Button btnCivicWebLogin, btnGetRecords,
            btnCreateRecord, btnSearchRecord,
            btnCivicLogout, btnBack;
    private ViewGroup mainLayout = null;
    private AMRequest currentRequest = null;
    private AccelaMobile accelaMobile = null;
    private ProgressDialog progressDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize content view.
        setContentView(R.layout.activity_citizen_test);
        initContentView();

        // Create an AccelaMobile instance with the App ID and App Secret of the
        // registered app.
        this.accelaMobile = AccelaMobile.getInstance();
        this.accelaMobile.initialize(getApplicationContext(), this.AppID, this.AppSecret, AccelaMobile.Environment.PROD, sessionDelegate);

    }

    public void onClick(View view) {

        String servicePath = null;
        RequestParams requestParams = new RequestParams();
        AMRequestSender request = this.accelaMobile.getRequestSender();
        switch (view.getId()) {
            case R.id.btnCivicWebLogin:
                this.accelaMobile.getAuthorizationManager().showAuthorizationWebView(this, Permissions4Authorization, URL_SCHEMA, AGENCY);
                break;
            case R.id.btnCivicGetRecords:
                if (isSessionValid()) {
                    servicePath = SERVICE_URI_RECORD_LIST;
                    requestParams.put("limit", "10");
                    requestParams.put("offset", "0");
                    currentRequest = request.sendRequest(servicePath, requestParams, null, requestDelegate);
                }
                break;
            case R.id.btnCivicCreateRecord:
                if (isSessionValid()) {
                    servicePath = SERVICE_URI_RECORD_CREATE;
                    JSONObject recordJson = populateRecordJson();
                    RequestParams searchOptions = new RequestParams(recordJson);
                    currentRequest = request.sendRequest(
                            servicePath, null, null, HTTPMethod.POST, searchOptions,
                            requestDelegate);
                }
                break;
            case R.id.btnCivicSearchRecord:
                if (isSessionValid()) {
                    servicePath = SERVICE_URI_RECORD_SEARCH;
                    RequestParams searchOptions = new RequestParams();
                    searchOptions.put("module", "Building");
                    Map<String, String> map = new HashMap<>();
                    map.put("limit", "10");
                    map.put("offset", "0");
                    map.put("expand", "Addresses");
                    RequestParams urlParams = new RequestParams();
                    urlParams.setUrlParams(map);
                    currentRequest = request.sendRequest(servicePath, urlParams, null, HTTPMethod.POST, searchOptions, requestDelegate);
                }
                break;
            case R.id.btnCivicLogout:
                if (isSessionValid()) {
                    this.accelaMobile.getAuthorizationManager().logout();
                }
                break;
            case R.id.btnCivicBack:
                this.onBackPressed();
        }
    }

    private void initContentView() {
        // Initialize UI elements.
        this.mainLayout = (ViewGroup) this.findViewById(android.R.id.content)
                .getRootView();
        this.btnCivicWebLogin = (Button) this
                .findViewById(R.id.btnCivicWebLogin);
        this.btnGetRecords = (Button) this
                .findViewById(R.id.btnCivicGetRecords);
        this.btnCreateRecord = (Button) this
                .findViewById(R.id.btnCivicCreateRecord);

        this.btnSearchRecord = (Button) this
                .findViewById(R.id.btnCivicSearchRecord);
        this.btnCivicLogout = (Button) this.findViewById(R.id.btnCivicLogout);
        this.btnBack = (Button) this.findViewById(R.id.btnCivicBack);
        // Set events for buttons.
        this.btnCivicWebLogin.setOnClickListener(this);
        this.btnGetRecords.setOnClickListener(this);
        this.btnCreateRecord.setOnClickListener(this);
        this.btnSearchRecord.setOnClickListener(this);
        this.btnCivicLogout.setOnClickListener(this);
        this.btnBack.setOnClickListener(this);
    }

    // Create and show an alert dialog
    private void createAlertDialog(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing except closing itself.
                    }
                }).create().show();
    }

    // Check whether the current user session is valid or not.
    private boolean isSessionValid() {
        Boolean isValid = this.accelaMobile.isSessionValid();
        if (!isValid) {
            Toast.makeText(this,
                    this.getResources().getString(R.string.msg_not_logged_in),
                    Toast.LENGTH_SHORT).show();
        }
        return isValid;
    }

    // Session delegate for Accela Mobile
    private AMSessionDelegate sessionDelegate = new AMSessionDelegate() {
        public void amDidLogin() {
            // Dismiss progress dialog.
            if ((accessTokenProgressDialog != null)
                    && (accessTokenProgressDialog.isShowing())) {
                accessTokenProgressDialog.dismiss();
            }
            // Show message.
            Toast.makeText(
                    CitizenTestActivity.this,
                    CitizenTestActivity.this.getResources().getString(
                            R.string.msg_logged_in), Toast.LENGTH_SHORT).show();
            AMLogger.logInfo("In CitizenTestActivity: Session Delegate.amDidLogin() invoked...");
        }

        public void amDidLoginFailure(AMError error) {
            Toast.makeText(
                    CitizenTestActivity.this,
                    CitizenTestActivity.this.getResources().getString(
                            R.string.msg_login_failed), Toast.LENGTH_SHORT)
                    .show();
            AMLogger.logInfo(
                    "In CitizenTestActivity: Session Delegate.amDidLoginFailure() invokded: %s",
                    error.toString());
        }

        public void amDidCancelLogin() {
            AMLogger.logInfo("In CitizenTestActivity: Session Delegate.amDidCancelLogin() invoked...");
        }

        public void amDidSessionInvalid(AMError error) {
            AMLogger.logInfo(
                    "In CitizenTestActivity: Session Delegate.amDidSessionInvalid() invoked: %s",
                    error.toString());
        }

        public void amDidLogout() {
            Toast.makeText(
                    CitizenTestActivity.this,
                    CitizenTestActivity.this.getResources().getString(
                            R.string.msg_logged_out), Toast.LENGTH_SHORT)
                    .show();
            AMLogger.logInfo("In CitizenTestActivity: Session Delegate.amDidLogout() invoked...");
        }
    };

    private void dismissProgressDialog() {
        // Dismiss the process waiting view
        if ((progressDialog != null) && (progressDialog.isShowing())) {
            progressDialog.dismiss();
        }
    }

    private void showProgressDialog(String message) {
        progressDialog = ProgressDialog.show(CitizenTestActivity.this, null, message);
    }

    /**
     * private variable, defined the request delegate to be used by normal
     * request.
     */
    private AMRequestDelegate requestDelegate = new AMRequestDelegate() {
        @Override
        public void onStart() {
            amRequestStarted(currentRequest);
            // Show progress waiting view
            showProgressDialog(CitizenTestActivity.this.getResources().getString(R.string.msg_request_being_processed));
        }

        @Override
        public void onSuccess(JSONObject responseJson) {
            amRequestDidReceiveResponse(currentRequest);
            amRequestDidLoad(currentRequest, responseJson);
            // Dismiss the process waiting view
            dismissProgressDialog();
            // Show dialog with the retrured Json data
            createAlertDialog(
                    CitizenTestActivity.this.getResources().getString(
                            R.string.msg_request_completed_title),
                    CitizenTestActivity.this.getResources().getString(
                            R.string.msg_request_completed_message)
                            + ": \n " + responseJson.toString());
        }

        @Override
        public void onFailure(AMError error) {
            amRequestDidReceiveResponse(currentRequest);
            // Dismiss the process waiting view
            dismissProgressDialog();
            AMError amError = new AMError(error.getStatus(), errorMessage,
                    traceId, null, null);
            // Show dialog with the returned error
            createAlertDialog(CitizenTestActivity.this.getResources()
                            .getString(R.string.error_request_failed_title),
                    amError.toString());
        }
    };

    /**
     * private method, used to populate the JSON data for the request which
     * creates record.
     */
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
}

