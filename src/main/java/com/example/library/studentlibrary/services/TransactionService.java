package com.example.library.studentlibrary.services;

import com.example.library.studentlibrary.models.*;
import com.example.library.studentlibrary.repositories.BookRepository;
import com.example.library.studentlibrary.repositories.CardRepository;
import com.example.library.studentlibrary.repositories.TransactionRepository;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TransactionService {

    @Autowired
    BookRepository bookRepository5;

    @Autowired
    CardRepository cardRepository5;

    @Autowired
    TransactionRepository transactionRepository5;

    @Value("${books.max_allowed}")
    int max_allowed_books;

    @Value("${books.max_allowed_days}")
    int getMax_allowed_days;

    @Value("${books.fine.per_day}")
    int fine_per_day;

    public String issueBook(int cardId, int bookId) throws Exception {
        //check whether bookId and cardId already exist
        //conditions required for successful transaction of issue book:
        //1. book is present and available
        if(bookRepository5.findByAvailability(true).get(bookId).isAvailable()){
            //bookRepository5.findById(bookId).get().setAvailable(false);
            bookRepository5.updateBook(bookRepository5.findById(bookId).get());
        }
        // If it fails: throw new Exception("Book is either unavailable or not present");
        else {
            throw new Exception("Book is either unavailable or not present");
        }
        //2. card is present and activated
        if(cardRepository5.findById(cardId).get().getCardStatus()==CardStatus.ACTIVATED){
            cardRepository5.findById(cardId).get().getBooks().add(bookRepository5.findById(bookId).get());
        }
        // If it fails: throw new Exception("Card is invalid");
        else {
            throw new Exception("Card is invalid");
        }
        //3. number of books issued against the card is strictly less than max_allowed_books
        Transaction transaction;
        if(cardRepository5.findById(cardId).get().getBooks().size()<max_allowed_books){
            //List<Transaction> transactions=transactionRepository5.find(cardId,bookId,TransactionStatus.SUCCESSFUL,true);
            transaction=Transaction.builder().transactionDate(new Date()).
                    transactionStatus(TransactionStatus.SUCCESSFUL).
                    book(bookRepository5.findById(bookId).get()).
                    card(cardRepository5.findById(cardId).get()).
                    isIssueOperation(true).
                    build();
            transactionRepository5.save(transaction);
            transactionRepository5.find(cardId,bookId,TransactionStatus.SUCCESSFUL,true).add(transaction);
        }
        // If it fails: throw new Exception("Book limit has reached for this card");
        else{
            throw new Exception("Book limit has reached for this card");
        }
        //If the transaction is successful, save the transaction to the list of transactions and return the id

        //Note that the error message should match exactly in all cases

        //return transactionId instead
        return transaction.getTransactionId();
    }

    public Transaction returnBook(int cardId, int bookId) throws Exception{

        List<Transaction> transactions = transactionRepository5.find(cardId, bookId,TransactionStatus.SUCCESSFUL, true);
        Transaction transaction = transactions.get(transactions.size() - 1);

        //for the given transaction calculate the fine amount considering the book has been returned exactly when this function is called

        Date date=new Date();
        long dif=date.getTime()-transaction.getTransactionDate().getTime();
        int amount=0;
        if(TimeUnit.DAYS.convert(dif,TimeUnit.MILLISECONDS)>getMax_allowed_days){
            amount+=(TimeUnit.DAYS.convert(dif,TimeUnit.MILLISECONDS)-getMax_allowed_days)*fine_per_day;
        }
        //make the book available for other users
        cardRepository5.findById(cardId).get().getBooks().remove(bookRepository5.findById(bookId).get());
        bookRepository5.updateBook(bookRepository5.findById(bookId).get());
        //make a new transaction for return book which contains the fine amount as well

        Transaction returnBookTransaction  = Transaction.builder().
                transactionStatus(TransactionStatus.SUCCESSFUL).
                fineAmount(amount).
                book(bookRepository5.findById(bookId).get()).
                card(cardRepository5.findById(cardId).get()).
                build();
        return returnBookTransaction; //return the transaction after updating all details
    }
}