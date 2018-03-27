package ua.`in`.smartjava.utils

import com.amazonaws.AmazonClientException
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.profile.ProfileCredentialsProvider

class CredentialUtils {

    val credentialsProvider: AWSCredentialsProvider
        get() {
            val credentialsProvider: AWSCredentialsProvider
            credentialsProvider = try {
                ProfileCredentialsProvider("default")
            } catch (e: Exception) {
                throw AmazonClientException("Cannot load the credentials(~/.aws/credentials)")
            }
            return credentialsProvider
        }
}
