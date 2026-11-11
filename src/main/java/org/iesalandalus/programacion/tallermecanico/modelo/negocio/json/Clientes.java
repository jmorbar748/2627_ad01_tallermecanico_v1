package org.iesalandalus.programacion.tallermecanico.modelo.negocio.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iesalandalus.programacion.tallermecanico.modelo.TallerMecanicoExcepcion;
import org.iesalandalus.programacion.tallermecanico.modelo.dominio.Cliente;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.IClientes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Clientes implements IClientes {

    private static final String FICHERO_CLIENTES = "datos/ficheros/json/clientes.json";

    private final ObjectMapper mapper = new ObjectMapper();

    private static Clientes instancia;

    private Clientes() {
    }

    public static Clientes getInstancia() {

        if (instancia == null) {
            instancia = new Clientes();
        }
        return  instancia;
    }

    @Override
    public void comenzar() {

    }

    @Override
    public void terminar() {

    }

    private List<Cliente> leer() {
        File file = new File(FICHERO_CLIENTES);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            return mapper.readValue(file, new TypeReference<List<Cliente>>() {});
        } catch (IOException e) {
            System.out.println("Error al leer el fichero de clientes: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void escribir(List<Cliente> clientes) {

        try {
            File archivo = new File(FICHERO_CLIENTES);

            File carpeta = archivo.getParentFile();
            if (carpeta != null && !carpeta.exists()) {
                carpeta.mkdirs();
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(archivo, clientes);

        } catch (IOException e) {
            System.out.println("Error al escribir en el fichero de clientes: " + e.getMessage());
        }
    }

    @Override
    public List<Cliente> get(){
        return leer();
    }

    @Override
    public void insertar(Cliente cliente) throws TallerMecanicoExcepcion{

        List<Cliente> clientes = leer();
        if (!clientes.contains(cliente)) {
            clientes.add(cliente);
            escribir(clientes);
        } else {
            System.out.println("El cliente ya existe.");
        }

    }

    @Override
    public Cliente modificar(Cliente cliente, String nombre, String telefono) throws TallerMecanicoExcepcion {
        List<Cliente> clientes = leer();

        int indice = clientes.indexOf(cliente);
        if (indice != -1) {
            Cliente clienteExistente = clientes.get(indice);

            if (nombre != null && !nombre.isBlank()) {
                clienteExistente.setNombre(nombre);
            }

            if (telefono != null && !telefono.isBlank()) {
                clienteExistente.setTelefono(telefono);
            }
            escribir(clientes);
            return clienteExistente; // Importante: devolver el cliente
        } else {
            throw new TallerMecanicoExcepcion("El cliente a modificar no existe.");
        }
    }

    @Override
    public Cliente buscar(Cliente cliente) {
        List<Cliente> clientes = leer();
        int indice = clientes.indexOf(cliente);
        if (indice != -1) {
            return clientes.get(indice);
        }
        return null;
    }

    @Override
    public void borrar(Cliente cliente) throws TallerMecanicoExcepcion{
        List<Cliente> clientes = leer();
        if (clientes.remove(cliente)) {
            escribir(clientes);
        } else {
            throw new TallerMecanicoExcepcion("El cliente a borrar no existe.");
        }
    }

}
