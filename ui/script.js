const clientMap = new Map();
const statsMap = new Map();

/*
interface Client {
    port: number,
    clientId: string
}
*/

const table = () => {
    const table = document.createElement("table");
    const headers = ["Client ID", "Total", "Updated"]

    const headerRow = table.insertRow(0);
    for (const value of headers) {
        const th = document.createElement("th");
        th.textContent = value;
        headerRow.appendChild(th);
    }

    let index = 1;
    statsMap.forEach((stats, client) => {
        const row = table.insertRow(index ++);
            const clientId = row.insertCell();
            clientId.textContent = client;
            const total = row.insertCell();
            total.textContent = stats.total;
            const updated = row.insertCell();
            updated.textContent = stats.updated;
    });

    // Append the table to the body
    document.getElementById("data").innerHTML = ""
    document.getElementById("data").appendChild(table);
} 

setInterval(() => table(), 2000)


setInterval(() => {
    clientMap.forEach((client, clientId) => {
        fetch(`http://localhost:${client.port}/stats`)
        .then(r => r.json())
        .then(stats => statsMap.set(clientId, stats))
    })
}, 5000)

setInterval(() => {
    fetch("http://localhost:8081/discover")
        .then(r => r.json())
        .then(clients => clients.forEach(c => clientMap.set(c.clientId, c)))}, 1000)