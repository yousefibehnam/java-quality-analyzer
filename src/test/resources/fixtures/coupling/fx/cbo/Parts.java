package fx.cbo;

class Customer {
}

class Line {
    Product product;
}

class Product {
}

class Money {
    static Money zero() {
        return new Money();
    }
}

class Report {
    void print(Order order) {
        order.total();
    }
}
