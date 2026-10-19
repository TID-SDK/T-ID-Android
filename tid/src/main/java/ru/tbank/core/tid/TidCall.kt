/*
 * Copyright © 2024 T-Bank
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.tbank.core.tid

import androidx.annotation.WorkerThread
import ru.tbank.core.tid.error.TidRequestException

/**
 * @author Stanislav Mukhametshin
 *
 * Main class to perform requests from T-Bank Api
 */
public interface TidCall<T> {

    /**
     * Function for synchronous operations
     *
     * NOTE should not be called from the main thread
     *
     * @return T
     *
     * @throws TidRequestException if something goes wrong.
     * It can contain message [TBankErrorMessage][ru.tbank.core.tid.error.TidErrorMessage]
     * with problem description
     */
    @WorkerThread
    @Throws(TidRequestException::class)
    public fun getResponse(): T

    /**
     * Function to cancel [TidCall]
     */
    public fun cancel()
}
