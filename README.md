# Car Rental User App

An Android application for users to rent cars, with both Razorpay and Wallet payment options.

## Payment System

The app implements a dual payment system:

### 1. Razorpay Integration

The Razorpay payment gateway handles all payment processing tasks including:
- Credit/Debit card payments
- UPI payments
- Net Banking
- Wallet payments from external providers

Key features:
- Secure payment processing
- Transaction records saved to Firestore
- Payment confirmation and receipts
- Test mode for development

### 2. In-App Wallet

The app includes a built-in wallet system for users to:
- Store funds in their account balance
- Make payments directly from their wallet
- Receive refunds when bookings are canceled
- View transaction history

When a user selects the wallet payment option:
- The system checks if sufficient balance is available
- Deducts the amount from the wallet balance
- Creates a transaction record
- Updates the booking with payment details

## How to Use

1. **Payment Flow**:
   - Select a car to rent
   - Choose booking details
   - On payment screen, select either Razorpay or Wallet
   - For wallet payments, ensure sufficient balance

2. **Managing Wallet**:
   - Add funds to wallet using Razorpay
   - View transaction history
   - Check current balance

## Technical Implementation

- Firebase Firestore used for storing payment data
- Transaction records include detailed metadata
- Atomic operations ensure data consistency
- Real-time balance updates 