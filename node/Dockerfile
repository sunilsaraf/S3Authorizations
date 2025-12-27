FROM golang:1.20-alpine
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN go build -o /usr/local/bin/blockchain-node ./cmd/node
EXPOSE 8080
CMD ["/usr/local/bin/blockchain-node"]