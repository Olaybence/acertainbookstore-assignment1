package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.BookRating;
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * {@link BookStoreTest} tests the {@link BookStore} interface.
 * 
 * @see BookStore
 */
public class BookStoreTest {

	/** The Constant TEST_ISBN. */
	private static final int TEST_ISBN = 3044560;

	/** The Constant NUM_COPIES. */
	private static final int NUM_COPIES = 5;

	/** The local test. */
	private static boolean localTest = true;

	/** The store manager. */
	private static StockManager storeManager;

	/** The client. */
	private static BookStore client;

	/**
	 * Sets the up before class.
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		try {
			String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
			localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;

			if (localTest) {
				CertainBookStore store = new CertainBookStore();
				storeManager = store;
				client = store;
			} else {
				storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
				client = new BookStoreHTTPProxy("http://localhost:8081");
			}

			storeManager.removeAllBooks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to add some books.
	 *
	 * @param isbn
	 *            the isbn
	 * @param copies
	 *            the copies
	 * @throws BookStoreException
	 *             the book store exception
	 */
	public void addBooks(int isbn, int copies) throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		StockBook book = new ImmutableStockBook(isbn, "Test of Thrones", "George RR Testin'", (float) 10, copies, 0, 0,
				0, false);
		booksToAdd.add(book);
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Helper method to get the default book used by initializeBooks.
	 *
	 * @return the default book
	 */
	public StockBook getDefaultBook() {
		return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit", "JK Unit", (float) 10, NUM_COPIES, 0, 0, 0,
				false);
	}

	/**
	 * Method to add a book, executed before every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Before
	public void initializeBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Method to clean up the book store, execute after every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@After
	public void cleanupBooks() throws BookStoreException {
		storeManager.removeAllBooks();
	}

	/**
	 * Tests basic buyBook() functionality.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyAllCopiesDefaultBook() throws BookStoreException {
		// Set of books to buy
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

		// Try to buy books
		client.buyBooks(booksToBuy);

		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 1);
		StockBook bookInList = listBooks.get(0);
		StockBook addedBook = getDefaultBook();

		assertTrue(bookInList.getISBN() == addedBook.getISBN() && bookInList.getTitle().equals(addedBook.getTitle())
				&& bookInList.getAuthor().equals(addedBook.getAuthor()) && bookInList.getPrice() == addedBook.getPrice()
				&& bookInList.getNumSaleMisses() == addedBook.getNumSaleMisses()
				&& bookInList.getAverageRating() == addedBook.getAverageRating()
				&& bookInList.getNumTimesRated() == addedBook.getNumTimesRated()
				&& bookInList.getTotalRating() == addedBook.getTotalRating()
				&& bookInList.isEditorPick() == addedBook.isEditorPick());
	}

	/**
	 * Tests that books with invalid ISBNs cannot be bought.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with invalid ISBN.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(-1, 1)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that books can only be bought if they are in the book store.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNonExistingISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with ISBN which does not exist.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(100000, 10)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy more books than there are copies.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyTooManyBooks() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy more copies than there are in store.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy a negative number of books.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNegativeNumberOfBookCopies() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a negative number of copies.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that all books can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetBooks() throws BookStoreException {
		Set<StockBook> booksAdded = new HashSet<StockBook>();
		booksAdded.add(getDefaultBook());

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		booksAdded.addAll(booksToAdd);

		storeManager.addBooks(booksToAdd);

		// Get books in store.
		List<StockBook> listBooks = storeManager.getBooks();

		// Make sure the lists equal each other.
		assertTrue(listBooks.containsAll(booksAdded) && listBooks.size() == booksAdded.size());
	}

	/**
	 * Tests that a list of books with a certain feature can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetCertainBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		storeManager.addBooks(booksToAdd);

		// Get a list of ISBNs to retrieved.
		Set<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN + 1);
		isbnList.add(TEST_ISBN + 2);

		// Get books with that ISBN.
		List<Book> books = client.getBooks(isbnList);

		// Make sure the lists equal each other
		assertTrue(books.containsAll(booksToAdd) && books.size() == booksToAdd.size());
	}
	
	/**
	 * Helper method to add some books.
	 *
	 * @param isbn
	 *            the isbn
	 * @param copies
	 *            the copies
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void rateBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();

		// Add books to store
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", 
				"Donald Knuth", (float) 300, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 3, "ava for Absolute Beginners",
				"Iuliana Cosmina", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 4, "Expert Python Programming",
				"Tarek Ziade and Michal Jaworski", (float) 50, NUM_COPIES, 0, 0, 0, false));
		storeManager.addBooks(booksToAdd);
		
		// Rating 1
		HashSet<BookRating> bookRating = new HashSet<BookRating>();
		bookRating.add(new BookRating(TEST_ISBN + 1, 3) );
		bookRating.add(new BookRating(TEST_ISBN + 2, 5) );
		bookRating.add(new BookRating(TEST_ISBN + 3, 2) );
		client.rateBooks(bookRating);

		// Rating 2
		bookRating = new HashSet<BookRating>();
		bookRating.add(new BookRating(TEST_ISBN + 1, 5) );
		bookRating.add(new BookRating(TEST_ISBN + 2, 1) );
		client.rateBooks(bookRating);
		
		// TESTS ////////
		
		// TEST_ISBN + 1 --> 3,5 = 4
		Set<Integer> isbns = new HashSet<Integer>();
		isbns.add(TEST_ISBN + 1);
		List<StockBook> ratedBooks = storeManager.getBooksByISBN(isbns);
		
		assertTrue(ratedBooks.get(0).getAverageRating() == 4);

		// TEST_ISBN + 2 --> 5,1 = 3
		isbns = new HashSet<Integer>();
		isbns.add(TEST_ISBN + 2);
		ratedBooks = storeManager.getBooksByISBN(isbns);
		
		assertTrue(ratedBooks.get(0).getAverageRating() == 3);
		
		// TEST_ISBN + 3 --> 2
		isbns = new HashSet<Integer>();
		isbns.add(TEST_ISBN + 3);
		ratedBooks = storeManager.getBooksByISBN(isbns);
		
		assertTrue(ratedBooks.get(0).getAverageRating() == 2);
		
		// TEST_ISBN + 3 --> 2
		isbns = new HashSet<Integer>();
		isbns.add(TEST_ISBN);
		ratedBooks = storeManager.getBooksByISBN(isbns);
		
		assertTrue(ratedBooks.get(0).getAverageRating() == -1);
	}
	
	/**
	 * Tests that a list of books with a certain feature can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetTopRatedBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", 
				"Donald Knuth", (float) 300, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 3, "ava for Absolute Beginners",
				"Iuliana Cosmina", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 4, "Expert Python Programming",
				"Tarek Ziade and Michal Jaworski", (float) 50, NUM_COPIES, 0, 0, 0, false));

		storeManager.addBooks(booksToAdd);

		HashSet<BookRating> bookRating = new HashSet<BookRating>();
		bookRating.add(new BookRating(TEST_ISBN + 1, 5) );
		bookRating.add(new BookRating(TEST_ISBN + 2, 5) );
		bookRating.add(new BookRating(TEST_ISBN + 3, 3) );
		bookRating.add(new BookRating(TEST_ISBN + 4, 1) );
		client.rateBooks(bookRating);
		
		
		bookRating = new HashSet<BookRating>();
		bookRating.add(new BookRating(TEST_ISBN + 1, 5) );
		bookRating.add(new BookRating(TEST_ISBN + 2, 4) );
		client.rateBooks(bookRating);
		
		
		bookRating = new HashSet<BookRating>();
		bookRating.add(new BookRating(TEST_ISBN + 1, 5) );
		client.rateBooks(bookRating);
		
		
		
		// Get books with that ISBN.
		int numBooks = 3;
		List<Book> topBooks = client.getTopRatedBooks(numBooks);
		
		// Create the supposed list of books
		List<StockBook> topRateds = new ArrayList<StockBook>();
		topRateds.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", 
				"Donald Knuth", (float) 300, NUM_COPIES, 0, 0, 0, false));
		
		topRateds.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		topRateds.add(new ImmutableStockBook(TEST_ISBN + 3, "ava for Absolute Beginners",
				"Iuliana Cosmina", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		// Make sure the lists equal each other
		assertTrue(topBooks.equals(topRateds));
	}
	
	/**
	 * Test what getTopRated(3) gives back if there is enough books but none is rated.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetTopRatedWithNoRatings() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", 
				"Donald Knuth", (float) 300, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 3, "ava for Absolute Beginners",
				"Iuliana Cosmina", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 4, "Expert Python Programming",
				"Tarek Ziade and Michal Jaworski", (float) 50, NUM_COPIES, 0, 0, 0, false));

		storeManager.addBooks(booksToAdd);
		
		List<StockBook> booksInStorePreTest = storeManager.getBooks();
		
		// Get books with that ISBN.
		int numBooks = 2;
		try {			
			client.getTopRatedBooks(numBooks);
			fail();
		} catch(BookStoreException e) {
			;
		}
		
		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}
	
	/**
	 * Tests getBooksInDemand() function.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetBooksInDemand() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", 
				"Donald Knuth", (float) 300, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 3, "ava for Absolute Beginners",
				"Iuliana Cosmina", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 4, "Expert Python Programming",
				"Tarek Ziade and Michal Jaworski", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		storeManager.addBooks(booksToAdd);
		
		
		// Try to buy a book with invalid ISBN.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN + 1, 1000));
		booksToBuy.add(new BookCopy(TEST_ISBN + 2, 2)); 
		booksToBuy.add(new BookCopy(TEST_ISBN + 3, 1000));

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInDemand = storeManager.getBooksInDemand();

		List<StockBook> demanded = new ArrayList<StockBook>();
		demanded.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", 
				"Donald Knuth", (float) 300, NUM_COPIES, 0, 0, 0, false));
		demanded.add(new ImmutableStockBook(TEST_ISBN + 3, "ava for Absolute Beginners",
				"Iuliana Cosmina", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		// Check pre and post state are same.
		assertTrue(booksInDemand.containsAll(demanded)
				&& booksInDemand.size() == demanded.size());
	}
	
	/**
	 * Tests getBooksInDemand() function.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testNoBooksInDemand() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", 
				"Donald Knuth", (float) 300, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 3, "ava for Absolute Beginners",
				"Iuliana Cosmina", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 4, "Expert Python Programming",
				"Tarek Ziade and Michal Jaworski", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		storeManager.addBooks(booksToAdd);
		
		
		// Try to buy a book with invalid ISBN.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN + 1, 1));
		booksToBuy.add(new BookCopy(TEST_ISBN + 2, 2));
		booksToBuy.add(new BookCopy(TEST_ISBN + 3, 1));

		client.buyBooks(booksToBuy);

		List<StockBook> booksInDemand = storeManager.getBooksInDemand();
		
		// Check pre and post state are same.
		assertTrue(booksInDemand.isEmpty());
	}
	
	/**
	 * Tests getBooksInDemand() function with no missing book.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testEmptyDemand() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", 
				"Donald Knuth", (float) 300, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 3, "ava for Absolute Beginners",
				"Iuliana Cosmina", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 4, "Expert Python Programming",
				"Tarek Ziade and Michal Jaworski", (float) 50, NUM_COPIES, 0, 0, 0, false));
		
		storeManager.addBooks(booksToAdd);
		
		
		// Try to buy a book with invalid ISBN.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN + 1, 1));
		booksToBuy.add(new BookCopy(TEST_ISBN + 2, 2));
		booksToBuy.add(new BookCopy(TEST_ISBN + 3, 3));

		client.buyBooks(booksToBuy);

		List<StockBook> booksInDemand = storeManager.getBooksInDemand();
		
		// Check pre and post state are same.
		assertTrue(booksInDemand.size() == 0);
	}

	/**
	 * Tests that books cannot be retrieved if ISBN is invalid.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetInvalidIsbn() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Make an invalid ISBN.
		HashSet<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN); // valid
		isbnList.add(-1); // invalid

		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.getBooks(isbnList);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tear down after class.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws BookStoreException {
		storeManager.removeAllBooks();

		if (!localTest) {
			((BookStoreHTTPProxy) client).stop();
			((StockManagerHTTPProxy) storeManager).stop();
		}
	}
}
