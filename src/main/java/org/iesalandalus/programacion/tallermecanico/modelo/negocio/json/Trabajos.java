package org.iesalandalus.programacion.tallermecanico.modelo.negocio.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.iesalandalus.programacion.tallermecanico.modelo.TallerMecanicoExcepcion;
import org.iesalandalus.programacion.tallermecanico.modelo.dominio.*;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.ITrabajos;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

public class Trabajos implements ITrabajos {

    private static final String FICHERO_TRABAJOS = "datos/ficheros/json/trabajos.json";

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.findAndRegisterModules();
    }

    private static Trabajos instancia;

    private Trabajos() {

    }

    public static Trabajos getInstancia() {
        if (instancia == null) {
            instancia = new Trabajos();
        }
        return instancia;
    }

    @Override
    public void comenzar() {

    }

    @Override
    public void terminar() {

    }

    private List<Trabajo> leer() {
        File file = new File(FICHERO_TRABAJOS);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper mapperLocal = new ObjectMapper();
            mapperLocal.registerModule(new JavaTimeModule());
            mapperLocal.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

            // Leemos el JSON como una lista de mapas
            List<Map<String, Object>> listaJson = mapperLocal.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
            List<Trabajo> trabajos = new ArrayList<>();

            for (Map<String, Object> mapa : listaJson) {
                String tipo = (String) mapa.get("tipoTrabajo");
                Cliente cliente = mapperLocal.convertValue(mapa.get("cliente"), Cliente.class);
                Vehiculo vehiculo = mapperLocal.convertValue(mapa.get("vehiculo"), Vehiculo.class);
                LocalDate fechaInicio = LocalDate.parse((String) mapa.get("fechaInicio"));
                LocalDate fechaFin = mapa.get("fechaFin") != null ? LocalDate.parse((String) mapa.get("fechaFin")) : null;
                int horas = (Integer) mapa.get("horas");

                Trabajo trabajo = null;
                if ("revision".equals(tipo)) {
                    trabajo = new Revision(cliente, vehiculo, fechaInicio);
                } else if ("mecanico".equals(tipo)) {
                    trabajo = new Mecanico(cliente, vehiculo, fechaInicio);
                    float precioMaterial = ((Number) mapa.get("precioMaterial")).floatValue();
                    if (precioMaterial > 0){
                        ((Mecanico) trabajo).anadirPrecioMaterial(precioMaterial);
                    }
                }

                if (trabajo != null) {
                    if (horas > 0) {
                        trabajo.anadirHoras(horas);
                    }
                    if (fechaFin != null) {
                        trabajo.cerrar(fechaFin);
                    }
                    trabajos.add(trabajo);
                }
            }
            return trabajos;
        } catch (IOException | TallerMecanicoExcepcion e) {
            System.out.println("Error al leer el fichero de trabajos: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void escribir(List<Trabajo> trabajos) {
        try {
            File archivo = new File(FICHERO_TRABAJOS);
            File carpeta = archivo.getParentFile();
            if (carpeta != null && !carpeta.exists()) {
                carpeta.mkdirs();
            }

            // Creamos un ObjectMapper normal (sin static) para escribir
            ObjectMapper mapperLocal = new ObjectMapper();
            mapperLocal.registerModule(new JavaTimeModule());
            mapperLocal.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

            // Construimos manualmente el JSON con el campo "tipoTrabajo"
            List<Map<String, Object>> listaJson = new ArrayList<>();
            for (Trabajo trabajo : trabajos) {
                Map<String, Object> mapa = new LinkedHashMap<>();
                if (trabajo instanceof Revision) {
                    mapa.put("tipoTrabajo", "revision");
                } else if (trabajo instanceof Mecanico) {
                    mapa.put("tipoTrabajo", "mecanico");
                }
                mapa.put("cliente", trabajo.getCliente());
                mapa.put("vehiculo", trabajo.getVehiculo());
                mapa.put("fechaInicio", trabajo.getFechaInicio());
                mapa.put("fechaFin", trabajo.getFechaFin());
                mapa.put("horas", trabajo.getHoras());
                if (trabajo instanceof Mecanico mecanico) {
                    mapa.put("precioMaterial", mecanico.getPrecioMaterial());
                }
                listaJson.add(mapa);
            }

            mapperLocal.writerWithDefaultPrettyPrinter().writeValue(archivo, listaJson);

        } catch (IOException e) {
            System.out.println("Error al escribir en el fichero de trabajos: " + e.getMessage());
        }
    }

    @Override
    public List<Trabajo> get() {
        return leer();
    }

    @Override
    public List<Trabajo> get(Cliente cliente) {
        List<Trabajo> trabajosCliente = new ArrayList<>();
        for (Trabajo trabajo : leer()) {
            if (trabajo.getCliente().equals(cliente)) {
                trabajosCliente.add(trabajo);
            }
        }
        return trabajosCliente;
    }

    @Override
    public List<Trabajo> get(Vehiculo vehiculo) {
        List<Trabajo> trabajosVehiculo = new ArrayList<>();
        for (Trabajo trabajo : leer()) {
            if (trabajo.getVehiculo().equals(vehiculo)) {
                trabajosVehiculo.add(trabajo);
            }
        }
        return trabajosVehiculo;
    }

    @Override
    public Map<TipoTrabajo, Integer> getEstadisticasMensuales(LocalDate mes) {
        Objects.requireNonNull(mes, "El mes no puede ser nulo.");
        Map<TipoTrabajo, Integer> estadisticas = inicializarEstadisticas();
        for (Trabajo trabajo : leer()) {
            LocalDate fecha = trabajo.getFechaInicio();
            if (fecha.getMonthValue() == mes.getMonthValue() && fecha.getYear() == mes.getYear()) {
                TipoTrabajo tipoTrabajo = TipoTrabajo.get(trabajo);
                estadisticas.put(tipoTrabajo, estadisticas.get(tipoTrabajo) + 1);
            }
        }
        return estadisticas;
    }

    private Map<TipoTrabajo, Integer> inicializarEstadisticas() {
        Map<TipoTrabajo, Integer> estadisticas = new EnumMap<>(TipoTrabajo.class);
        for (TipoTrabajo tipoTrabajo : TipoTrabajo.values()) {
            estadisticas.put(tipoTrabajo, 0);
        }
        return estadisticas;
    }

    @Override
    public void insertar(Trabajo trabajo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No se puede insertar un trabajo nulo.");
        List<Trabajo> trabajos = leer();
        comprobarTrabajo(trabajos, trabajo.getCliente(), trabajo.getVehiculo(), trabajo.getFechaInicio());
        trabajos.add(trabajo);
        escribir(trabajos);
    }

    @Override
    public Trabajo buscar(Trabajo trabajo) {
        Objects.requireNonNull(trabajo, "No se puede buscar un trabajo nulo.");
        List<Trabajo> trabajos = leer();
        int indice = trabajos.indexOf(trabajo);
        return (indice == -1) ? null : trabajos.get(indice);
    }

    @Override
    public void borrar(Trabajo trabajo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No se puede borrar un trabajo nulo.");
        List<Trabajo> trabajos = leer();
        if (!trabajos.remove(trabajo)) {
            throw new TallerMecanicoExcepcion("No existe ningún trabajo igual.");
        }
        escribir(trabajos);
    }

    @Override
    public Trabajo anadirHoras(Trabajo trabajo, int horas) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No puedo añadir horas a un trabajo nulo.");
        List<Trabajo> trabajos = leer();
        Trabajo trabajoEncontrado = getTrabajoAbierto(trabajos, trabajo.getVehiculo());
        trabajoEncontrado.anadirHoras(horas);
        escribir(trabajos);
        return trabajoEncontrado;
    }

    @Override
    public Trabajo anadirPrecioMaterial(Trabajo trabajo, float precioMaterial) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No puedo añadir precio del material a un trabajo nulo.");
        List<Trabajo> trabajos = leer();
        Trabajo trabajoEncontrado = getTrabajoAbierto(trabajos, trabajo.getVehiculo());
        if (trabajoEncontrado instanceof Mecanico mecanico) {
            mecanico.anadirPrecioMaterial(precioMaterial);
        } else {
            throw new TallerMecanicoExcepcion("No se puede añadir precio al material para este tipo de trabajos.");
        }
        escribir(trabajos);
        return trabajoEncontrado;
    }

    @Override
    public Trabajo cerrar(Trabajo trabajo, LocalDate fechaFin) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No puedo cerrar un trabajo nulo.");
        List<Trabajo> trabajos = leer();
        Trabajo trabajoEncontrado = getTrabajoAbierto(trabajos, trabajo.getVehiculo());
        trabajoEncontrado.cerrar(fechaFin);
        escribir(trabajos);
        return trabajoEncontrado;
    }

    private void comprobarTrabajo(List<Trabajo> trabajos, Cliente cliente, Vehiculo vehiculo, LocalDate fechaInicio) throws TallerMecanicoExcepcion {
        for (Trabajo trabajo : trabajos) {
            if (!trabajo.estaCerrado()) {
                if (trabajo.getCliente().equals(cliente)) {
                    throw new TallerMecanicoExcepcion("El cliente tiene otro trabajo en curso.");
                } else if (trabajo.getVehiculo().equals(vehiculo)) {
                    throw new TallerMecanicoExcepcion("El vehículo está actualmente en el taller.");
                }
            } else {
                if (trabajo.getCliente().equals(cliente) && !fechaInicio.isAfter(trabajo.getFechaFin())) {
                    throw new TallerMecanicoExcepcion("El cliente tiene otro trabajo posterior.");
                } else if (trabajo.getVehiculo().equals(vehiculo) && !fechaInicio.isAfter(trabajo.getFechaFin())) {
                    throw new TallerMecanicoExcepcion("El vehículo tiene otro trabajo posterior.");
                }
            }
        }
    }

    private Trabajo getTrabajoAbierto(List<Trabajo> trabajos, Vehiculo vehiculo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(vehiculo, "No puedo operar sobre un vehículo nulo.");
        Trabajo trabajoEncontrado = null;
        Iterator<Trabajo> iteradorTrabajos = trabajos.iterator();
        while (iteradorTrabajos.hasNext() && trabajoEncontrado == null) {
            Trabajo trabajo = iteradorTrabajos.next();
            if (trabajo.getVehiculo().equals(vehiculo) && !trabajo.estaCerrado()) {
                trabajoEncontrado = trabajo;
            }
        }
        if (trabajoEncontrado == null) {
            throw new TallerMecanicoExcepcion("No existe ningún trabajo abierto para dicho vehículo.");
        }
        return trabajoEncontrado;
    }

}
