package com.example.moviehub

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class ReviewValidatorTest {

    @Test
    fun `valid review passes validation`() {
        // Given
        val rating = 4.5f
        val comment = "Great movie! Loved the plot and acting."

        // When
        val isValid = validateReview(rating, comment)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `review with rating below minimum fails`() {
        // Given
        val rating = 0.0f
        val comment = "Bad movie"

        // When
        val isValid = validateReview(rating, comment)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `review with rating above maximum fails`() {
        // Given
        val rating = 6.0f
        val comment = "Too good to be true"

        // When
        val isValid = validateReview(rating, comment)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `review with empty comment fails`() {
        // Given
        val rating = 4.0f
        val comment = ""

        // When
        val isValid = validateReview(rating, comment)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `review with comment too short fails`() {
        // Given
        val rating = 4.0f
        val comment = "Bad"
        val minLength = 10

        // When
        val isValid = comment.length >= minLength

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `review with comment too long is truncated`() {
        // Given
        val rating = 4.0f
        val comment = "a".repeat(1001)
        val maxLength = 1000

        // When
        val truncated = comment.take(maxLength)

        // Then
        assertEquals(maxLength, truncated.length)
    }

    @Test
    fun `rating is rounded to nearest half star`() {
        // Given
        val ratings = listOf(3.2f, 3.3f, 3.7f, 3.8f)

        // When
        val rounded = ratings.map { roundToHalfStar(it) }

        // Then
        assertEquals(listOf(3.0f, 3.5f, 3.5f, 4.0f), rounded)
    }

    private fun validateReview(rating: Float, comment: String): Boolean {
        return rating in 0.5f..5.0f && comment.isNotBlank() && comment.length >= 10
    }

    private fun roundToHalfStar(rating: Float): Float {
        return (rating * 2).toInt() / 2.0f
    }
}