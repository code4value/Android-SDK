/**
 * Copyright 2015 Accela, Inc.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to
 * use, copy, modify, and distribute this software in source code or binary
 * form for use in connection with the web services and APIs provided by
 * Accela.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */
package com.accela.mobile.http;

import com.accela.mobile.http.volley.RetryPolicy;
import com.accela.mobile.http.volley.VolleyError;

/**
 * Created by eyang on 9/29/15.
 */
public class AMRetryPolicy implements RetryPolicy {
    /** The current timeout in milliseconds. */
    private int mCurrentTimeoutMs;

    /** The current retry count. */
    private int mCurrentRetryCount;

    /** The maximum number of attempts. */
    private final int mMaxNumRetries;

    /** The backoff multiplier for the policy. */
    private final float mBackoffMultiplier;

    /** The default socket timeout in milliseconds */
    public static final int DEFAULT_TIMEOUT_MS = 1000*60;//2500*4; //prolong to 10s for QA environment, the networking is very slow

    /** The default number of retries */
    public static final int DEFAULT_MAX_RETRIES = 1;

    /** The default backoff multiplier */
    public static final float DEFAULT_BACKOFF_MULT = 1f;

    /**
     * Constructs a new retry policy using the default timeouts.
     */
    public AMRetryPolicy() {
        this(DEFAULT_TIMEOUT_MS, DEFAULT_MAX_RETRIES, DEFAULT_BACKOFF_MULT);
    }

    /**
     * Constructs a new retry policy.
     * @param initialTimeoutMs The initial timeout for the policy.
     * @param maxNumRetries The maximum number of retries.
     * @param backoffMultiplier Backoff multiplier for the policy.
     */
    public AMRetryPolicy(int initialTimeoutMs, int maxNumRetries, float backoffMultiplier) {
        mCurrentTimeoutMs = initialTimeoutMs;
        mMaxNumRetries = maxNumRetries;
        mBackoffMultiplier = backoffMultiplier;
    }

    /**
     * Returns the current timeout.
     */
    @Override
    public int getCurrentTimeout() {
        return mCurrentTimeoutMs;
    }

    /**
     * Returns the current retry count.
     */
    @Override
    public int getCurrentRetryCount() {
        return mCurrentRetryCount;
    }

    /**
     * Returns the backoff multiplier for the policy.
     */
    public float getBackoffMultiplier() {
        return mBackoffMultiplier;
    }

    /**
     * Prepares for the next retry by applying a backoff to the timeout.
     * @param error The error code of the last attempt.
     */
    @Override
    public void retry(VolleyError error) throws VolleyError {
        mCurrentRetryCount++;
        mCurrentTimeoutMs += (mCurrentTimeoutMs * mBackoffMultiplier);
        if (!hasAttemptRemaining()) {
            throw error;
        }
    }

    /**
     * Returns true if this policy has attempts remaining, false otherwise.
     */
    protected boolean hasAttemptRemaining() {
        return mCurrentRetryCount <= mMaxNumRetries;
    }
}
